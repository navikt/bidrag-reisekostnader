package no.nav.bidrag.reisekostnad.tjeneste;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.time.LocalDateTime;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Databasetjeneste {

  private BarnDao barnDao;
  private ForelderDao forelderDao;
  private ForespørselDao forespørselDao;
  private Mapper mapper;

  @Autowired
  public Databasetjeneste(BarnDao barnDao, ForelderDao forelderDao, ForespørselDao forespørselDao, Mapper mapper) {
    this.barnDao = barnDao;
    this.forelderDao = forelderDao;
    this.forespørselDao = forespørselDao;
    this.mapper = mapper;
  }

  @Transactional
  public int lagreNyForespørsel(String hovedpart, String motpart, Set<String> identerBarn, boolean kreverSamtykke) {

    for (String ident : identerBarn) {
      var barn = barnDao.henteBarnTilknyttetAktivForespørsel(ident);
      if (barn.isPresent()) {
        log.warn("Validering feilet. Det finnes allerede en aktiv forespørsel for et av de oppgitte barna.");
        SIKKER_LOGG.warn("Validering feilet. Barn med ident {} er allerede tilknyttet en aktiv forespørsel", ident);
        throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_BARN_I_AKTIV_FORESPØRSEL);
      }
    }

    var ekisterendeHovedpart = forelderDao.finnMedPersonident(hovedpart);
    var eksisterendeMotpart = forelderDao.finnMedPersonident(motpart);

    var nyForespørsel = Forespørsel.builder().opprettet(LocalDateTime.now())
        .hovedpart(ekisterendeHovedpart.orElseGet(() -> Forelder.builder().personident(hovedpart).build()))
        .motpart(eksisterendeMotpart.orElseGet(() -> Forelder.builder().personident(motpart).build())).barn(mapper.tilEntitet(identerBarn))
        .kreverSamtykke(kreverSamtykke).build();

    var lagretForespørsel = forespørselDao.save(nyForespørsel);

    return lagretForespørsel.getId();
  }

  @Transactional
  public void giSamtykke(int idForespørsel, String personident) {
    log.info("Samtykker til fordeling av reisekostnader i forespørsel med id {}", idForespørsel);
    var forespørsel = forespørselDao.henteAktivForespørsel(idForespørsel);
    if (forespørsel.isPresent() && personident.equals(forespørsel.get().getMotpart().getPersonident())) {
      var nå = LocalDateTime.now();
      SIKKER_LOGG.info("Motpart (ident: {}) samtykker til fordeling av reisekostnader relatert til forespørsel id {}", personident, idForespørsel);
      forespørsel.get().setSamtykket(nå);
    } else {
      log.warn("Fant ikke forespørsel med id {}. Får ikke gitt samtykke.", idForespørsel);
      throw new Valideringsfeil(Feilkode.VALIDERING_SAMTYKKE_MOTPART);
    }
  }

  @Transactional
  public void deaktivereForespørsel(int idForespørsel, String personidentHovedpart) {
    log.info("Deaktiverer forespørsel med id {}", idForespørsel);
    var forespørsel = forespørselDao.henteAktivForespørsel(idForespørsel);
    if (forespørsel.isPresent() && personidentHovedpart.equals(forespørsel.get().getHovedpart().getPersonident())) {
      var nå = LocalDateTime.now();
      SIKKER_LOGG.info("Hovedpart (ident: {}) deaktiverer forespørsel med id {}", personidentHovedpart, idForespørsel);
      forespørsel.get().setDeaktivert(nå);
    } else {
      throw new Valideringsfeil(Feilkode.VALIDERING_DEAKTIVERE_HOVEDPART);
    }
  }
}
