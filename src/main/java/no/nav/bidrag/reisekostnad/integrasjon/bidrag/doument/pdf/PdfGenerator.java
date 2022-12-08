package no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import org.apache.commons.compress.utils.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class PdfGenerator {

  private static final String STI_TIL_PDF_TEMPLATE = "/pdf-template/";
  private static final Map<Elementnavn, String> elementnavnTilEngelsk = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(Elementnavn.BARN, "child"),
      new AbstractMap.SimpleEntry<>(Elementnavn.BESKRIVELSE, "description"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FØDSELSDATO, "date-of-birth"),
      new AbstractMap.SimpleEntry<>(Elementnavn.PERSONIDENT, "ssn"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FORNAVN, "first-name")
  );

  private static final Map<Elementnavn, String> nynorskeElementnavn = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(Elementnavn.BESKRIVELSE, "forklaring"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FORNAVN, "namn")
  );

  private static final Map<Tekst, String> tekstBokmål = Map.of(
      Tekst.FØDSELSDATO, "Fødselsdato",
      Tekst.PERSONIDENT, "Fødselsnummer",
      Tekst.FOEDESTED, "Fødested",
      Tekst.FORNAVN, "Navn",
      Tekst.OPPLYSNINGER_OM_BARNET, "Opplysninger om barnet"
  );

  private static final Map<Tekst, String> tekstNynorsk = Map.of(
      Tekst.FORNAVN, "Namn",
      Tekst.OPPLYSNINGER_OM_BARNET, "Opplysningar om barnet"
  );

  private static final Map<Tekst, String> tekstEngelsk = Map.of(
      Tekst.FØDSELSDATO, "Date of birth",
      Tekst.PERSONIDENT, "Social security number",
      Tekst.FORNAVN, "Name"
  );

  public static byte[] genererePdf(Set<PersonDto> barn, PersonDto hovedperson, PersonDto motpart) {

    var skriftspråk = Skriftspråk.BOKMÅL;

    log.info("Oppretter dokument for farskapserklæring på {}", skriftspråk);

    var html = byggeHtmlstrengFraMal(STI_TIL_PDF_TEMPLATE, skriftspråk, barn, hovedperson, motpart);
    try (final ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {

      var htmlSomStrøm = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
      org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlSomStrøm, "UTF-8", "pdf-template/template.html");
      Document doc = new W3CDom().fromJsoup(jsoupDoc);
      var builder = new PdfRendererBuilder();

      try (InputStream colorProfile = PdfGenerator.class.getResourceAsStream("/pdf-template/ISOcoated_v2_300_bas.ICC")) {
        byte[] colorProfileBytes = IOUtils.toByteArray(colorProfile);
        builder.useColorProfile(colorProfileBytes);
      }

      try (InputStream fontStream = PdfGenerator.class.getResourceAsStream("/pdf-template/Arial.ttf")) {
        final File midlertidigFil = File.createTempFile("Arial", "ttf");
        midlertidigFil.deleteOnExit();
        try (FileOutputStream ut = new FileOutputStream(midlertidigFil)) {
          IOUtils.copy(fontStream, ut);
        }
        builder.useFont(midlertidigFil, "ArialNormal");
      }

      try {
        builder.useProtocolsStreamImplementation(new ClassPathStreamFactory(), "classpath")
            .useFastMode()
            .usePdfAConformance(PdfAConformance.PDFA_2_A)
            .withW3cDocument(doc, "classpath:/pdf-template/")
            .toStream(pdfStream)
            .run();

      } catch (Exception e) {
        e.printStackTrace();
      }

      var innhold = pdfStream.toByteArray();
      pdfStream.close();

      return innhold;
    } catch (IOException ioe) {
      throw new InternFeil(Feilkode.PDF_OPPRETTELSE_FEILET, ioe);
    }
  }

  private static void leggeTilDataBarn(Element barnElement, Set<PersonDto> barna, Skriftspråk skriftspråk) {

    var beskrivelse = barnElement.getElementsByClass(henteElementnavn(Elementnavn.BESKRIVELSE, skriftspråk));
    var detaljer = barnElement.getElementsByClass(henteElementnavn(Elementnavn.DETALJER, skriftspråk)).first();
    var førsteBarn = barnElement.getElementById(henteElementnavn(Elementnavn.BARN_1, skriftspråk));

    var tekstformatBarn = "Fornavn: %s, fødselsdato: %s";
    var it = barna.iterator();
    var barn1 = it.next();
    førsteBarn.text(String.format(tekstformatBarn, barn1.getFornavn(),
        barn1.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

    var antallBarn = 1;
    while(it.hasNext()) {
      var barn = it.next();
       var nesteBarnIRekka = new Element("li");
      nesteBarnIRekka.id("barn_" +  ++antallBarn);

      var tekst = String.format(tekstformatBarn, barn.getFornavn(),
          barn.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
      nesteBarnIRekka.text(tekst);
      nesteBarnIRekka.appendTo(detaljer);
    }


    var t = false;
  }

  private static void leggeTilDataForelder(Element forelderelement, PersonDto forelder, Skriftspråk skriftspraak) {
    var navn = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.FORNAVN, skriftspraak));

    navn.first().text(tekstvelger(Tekst.FORNAVN, skriftspraak) + ": " + forelder.getFornavn());

    var foedselsdato = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.FØDSELSDATO, skriftspraak));
    foedselsdato.first().text(
        tekstvelger(Tekst.FØDSELSDATO, skriftspraak) + ": " + forelder.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    var foedselsnummer = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.PERSONIDENT, skriftspraak));
    foedselsnummer.first().text(tekstvelger(Tekst.PERSONIDENT, skriftspraak) + ": " + forelder.getIdent());
  }

  private static String byggeHtmlstrengFraMal(String pdfmal, Skriftspråk skriftspråk, Set<PersonDto> barn, PersonDto hovedperson, PersonDto motpart) {
    try {
      var input = new ClassPathResource(pdfmal + skriftspråk.toString().toLowerCase() + ".html").getInputStream();
      var document = Jsoup.parse(input, "UTF-8", "");

      var e = henteElementnavn(Elementnavn.BARN, skriftspråk);

      // Legge til informasjon om barn
      leggeTilDataBarn(document.getElementById(henteElementnavn(Elementnavn.BARN, skriftspråk)), barn, skriftspråk);
      // Legge til informasjon om mor
      leggeTilDataForelder(document.getElementById(henteElementnavn(Elementnavn.HOVEDPART, skriftspråk)), hovedperson, skriftspråk);
      // Legge til informasjon om far
      leggeTilDataForelder(document.getElementById(henteElementnavn(Elementnavn.MOTPART, skriftspråk)), motpart, skriftspråk);

      // jsoup fjerner tagslutt for <link> og <meta> - legger på manuelt ettersom dette er påkrevd av PDFBOX
      var html = document.html().replaceFirst("charset=utf-8\">", "charset=utf-8\"/>");
      html = html.replaceFirst("href=\"style.css\">", "href=\"style.css\"/>");

      return html;

    } catch (IOException ioe) {
      throw new InternFeil(Feilkode.PDF_OPPRETTELSE_FEILET, ioe);
    }
  }

  private static String henteElementnavn(Elementnavn element, Skriftspråk skriftspraak) {

    switch (skriftspraak) {
      case ENGELSK -> {
        return elementnavnTilEngelsk.get(element);
      }
      case NYNORSK -> {
        if (nynorskeElementnavn.containsKey(element)) {
          return nynorskeElementnavn.get(element);
        }
      }
    }

    // bokmål
    return element.toString().toLowerCase();
  }

  private static String tekstvelger(Tekst tekst, Skriftspråk skriftspråk) {
    switch (skriftspråk) {
      case ENGELSK -> {
        return tekstEngelsk.get(tekst);
      }
      case NYNORSK -> {
        if (tekstNynorsk.containsKey(tekst)) {
          return tekstNynorsk.get(tekst);
        } else {
          return tekstBokmål.get(tekst);
        }
      }
      default -> {
        return tekstBokmål.get(tekst);
      }
    }
  }

  private enum Tekst {
    FØDSELSDATO,
    PERSONIDENT,
    FOEDESTED,
    FORNAVN,
    OPPLYSNINGER_OM_BARNET,
    TERMINDATO;
  }

  private enum Elementnavn {
    BARN,
    BARN_1,
    BESKRIVELSE,
    DETALJER,
    MOTPART,
    FØDSELSDATO,
    PERSONIDENT,
    HOVEDPART,
    FORNAVN
  }
}