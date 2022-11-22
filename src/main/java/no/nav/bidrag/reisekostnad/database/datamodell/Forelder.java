package no.nav.bidrag.reisekostnad.database.datamodell;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Forelder implements Person, Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @NaturalId
  @Column(updatable = false)
  private String personident;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "hovedpart", cascade = CascadeType.MERGE)
  private final Set<Forespørsel> forespørslerHovdedpart = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "motpart", cascade = CascadeType.MERGE)
  private final Set<Forespørsel> forespørslerMotpart = new HashSet<>();

  @PreRemove
  private void fjerneHovedpart() {
    for (Forespørsel f : forespørslerHovdedpart) {
     //
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (personident == null ? 0 : personident.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Forelder other = (Forelder) obj;
    return personident.equals(other.personident);
  }
}