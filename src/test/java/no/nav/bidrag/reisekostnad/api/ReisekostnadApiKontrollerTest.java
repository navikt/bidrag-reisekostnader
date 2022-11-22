package no.nav.bidrag.reisekostnad.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import lombok.Value;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.Testkonfig;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("ReisekostnadApiKontrollerTest")
@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(stubs = "file:src/test/java/resources/wiremock/mappings", port = 0)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BidragReisekostnadApiTestapplikasjon.class, Testkonfig.class})
public class ReisekostnadApiKontrollerTest {

  private static final String KONTROLLERKONTEKST = "/api/v1/reisekostnad";
  private @Autowired MockOAuth2Server mockOAuth2Server;
  private @Autowired ServletWebServerApplicationContext webServerAppCtxt;
  @Autowired
  private @Qualifier("api") HttpHeaderTestRestTemplate httpHeaderTestRestTemplateApi;
  private @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;

  private static Testperson TESTPERSON_GRÅTASS = new Testperson("12345678910", "Gråtass", 40);
  private static Testperson TESTPERSON_STRENG = new Testperson("11111122222", "Streng", 38);
  private static Testperson TESTPERSON_BARN_16 = new Testperson("77777700000", "Grus", 16);
  private static Testperson TESTPERSON_BARN_10 = new Testperson("33333355555", "Småstein", 10);
  private static Testperson TESTPERSON_IKKE_FUNNET = new Testperson("00000001231", "Utenfor", 29);

  private static Testperson TESTPERSON_SERVERFEIL = new Testperson("12000001231", "Feil", 78);

  private static class CustomHeader {

    String headerName;
    String headerValue;

    CustomHeader(String headerName, String headerValue) {
      this.headerName = headerName;
      this.headerValue = headerValue;
    }
  }

  static <T> HttpEntity<T> initHttpEntity(T body, CustomHeader... customHeaders) {

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (customHeaders != null) {
      for (var header : customHeaders) {
        headers.add(header.headerName, header.headerValue);
      }
    }
    return new HttpEntity<>(body, headers);
  }

  private String generereTesttoken(String personident) {
    var token = mockOAuth2Server.issueToken("tokenx", personident, "aud-localhost");
    return "Bearer " + token.serialize();
  }

  @Nested
  class HenteBrukerinformasjon {

    private final static String ENDEPUNKT_BRUKERINFORMASJON = KONTROLLERKONTEKST + "/brukerinformasjon";
    private String urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;

    @Test
    void skalHenteBrukerinformasjonForHovedpartMedFamilierelasjoner() {

      // given
      var hovedperson = TESTPERSON_GRÅTASS;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(hovedperson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(hovedperson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // when
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // then
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getBrukersFornavn()).isEqualTo(hovedperson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
              .size()).isEqualTo(1));

      var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
      var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
          .stream().findFirst().get();
      var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

      assertAll(
          () -> assertThat(motpart.getFornavn()).isEqualTo(TESTPERSON_STRENG.getFornavn()),
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(TESTPERSON_STRENG.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(TESTPERSON_BARN_10.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(TESTPERSON_BARN_10.getFornavn()),
          () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(TESTPERSON_BARN_16.getFødselsdato()),
          () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(TESTPERSON_BARN_16.getFornavn()));
    }

    @Test
    void skalGiStatuskode404DersomPersondataMangler() {

      // given
      var hovedperson = TESTPERSON_IKKE_FUNNET;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(hovedperson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(hovedperson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // when
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // then
      assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void skalGiStatuskode500DersomKallMotBidragPersonFeilerMed500() {

      // given
      var hovedperson = TESTPERSON_SERVERFEIL;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(hovedperson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(hovedperson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // when
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // then
      assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Nested
  class OppretteForespørselOmFordelingAvReisekostnader {

    @Test
    void skalOppretteForespørselOmFordelingAvReisekostnadser() {

    }
  }

  @Nested
  class GiSamtykke {

  }
}

@Value
class Testperson {

  String ident;
  String fornavn;
  LocalDate fødselsdato;

  public Testperson(String ident, String fornavn, int alder) {
    this.ident = ident;
    this.fornavn = fornavn;
    this.fødselsdato = LocalDate.now().minusYears(alder);
  }
}