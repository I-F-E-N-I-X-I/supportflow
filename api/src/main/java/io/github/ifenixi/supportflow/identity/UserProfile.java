package io.github.ifenixi.supportflow.identity;
import jakarta.persistence.*;
@Entity @Table(name="user_profile")
public class UserProfile {
 @Id private Long userId;
 @MapsId @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private Account account;
 @Column(nullable=false,length=100) private String displayName;
 protected UserProfile() {}
 public UserProfile(Account account,String displayName){this.account=account;this.displayName=displayName;}
 public String getDisplayName(){return displayName;}
}
