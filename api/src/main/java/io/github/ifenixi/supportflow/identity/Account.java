package io.github.ifenixi.supportflow.identity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="app_user")
public class Account {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=64) private String username;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Role role;
 @Column(nullable=false) private Instant createdAt;
 @OneToOne(mappedBy="account",cascade=CascadeType.ALL,fetch=FetchType.LAZY,optional=false) private UserProfile profile;
 protected Account() {}
 public Account(String username,String hash,Role role) {this.username=username;passwordHash=hash;this.role=role;createdAt=Instant.now();profile=new UserProfile(this,username);}
 public Long getId(){return id;} public String getUsername(){return username;} public String getPasswordHash(){return passwordHash;} public Role getRole(){return role;}
 public UserProfile getProfile(){return profile;} public void changeRole(Role role){this.role=role;}
 public boolean isStaff(){return role!=Role.CLIENT;}
}
