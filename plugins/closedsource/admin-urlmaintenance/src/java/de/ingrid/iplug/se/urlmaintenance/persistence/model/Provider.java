package de.ingrid.iplug.se.urlmaintenance.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "getProviderByName", query = "select p from Provider as p where p._name = :name")
public class Provider extends IdBase {

  @Column(nullable = false, unique = true)
  private String _name;

  @ManyToOne
  @JoinColumn(nullable = false, name = "partner_fk")
  private Partner _partner;

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  public Partner getPartner() {
    return _partner;
  }

  public void setPartner(Partner partner) {
    _partner = partner;
  }

}
