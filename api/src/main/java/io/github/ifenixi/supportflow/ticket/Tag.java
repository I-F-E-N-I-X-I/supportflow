package io.github.ifenixi.supportflow.ticket;

import jakarta.persistence.*;

@Entity
public class Tag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String name;

  protected Tag() {}

  public Tag(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void rename(String name) {
    this.name = name;
  }
}
