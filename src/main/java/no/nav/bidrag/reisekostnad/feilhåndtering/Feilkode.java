package no.nav.bidrag.reisekostnad.feilhåndtering;

public enum Feilkode {

  DATABASEFEIL("Feil ved lesing fra eller skriving til database"),

  INTERNFEIL("Intern feil har oppstått."),
  VALIDERING_DEAKTIVERE_HOVEDPART("Fant ingen aktive forespørsler knyttet til oppgitt hovedpart."),
  VALIDERING_DEAKTIVERE_FEIL_STATUS("Den oppgitte forespørselen er allerede deaktivert"),
  KRYPTERING("Kryptering av personident feilet."),
  VALIDERING_PÅLOGGET_PERSON_DISKRESJON("Pålogget person har diskresjon. Kan ikke benytte løsningen."),
  VALIDERING_SAMTYKKE_MOTPART("Fant ingen aktive forespørsler knyttet til oppgitt motpart. Samtykke ikke oppdatert."),
  PDL_PERSON_DØD("Pålogget person er død."),
  PDL_PERSON_IKKE_FUNNET("Fant ikke person i PDL"),
  PDL_FEIL("En feil oppstod ved henting av data fra PDL"),
  VALIDERING_NY_FOREPØRSEL("Feil ved validering av ny forespørsel"),
  VALIDERING_NY_FOREPØRSEL_INGEN_FAMILIERELASJONER("Feil ved validering av ny forespørsel. Fant ingen familierelasjoner for person."),
  VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_MOTPART("Person mangler relasjon til oppgitt motpart"),
  VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_BARN("Person mangler relasjon til ett eller flere av de/ det oppgitte barna/ barnet"),

  VALIDERING_NY_FOREPØRSEL_BARN_I_AKTIV_FORESPØRSEL("Minst ett av det/de oppgitte barnet/ barna er tilknyttet en aktiv forepørsel.s");


  private final String beskrivelse;

  Feilkode(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public String getBeskrivelse() {
    return this.beskrivelse;
  }
}
