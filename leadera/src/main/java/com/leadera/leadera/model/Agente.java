package com.leadera.leadera.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="agente")
public class Agente implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, length = 255) // Aseguramos espacio para el Hash
    private String password;

    private String nombre;
    private String apellido;

    @ToString.Exclude
    @OneToMany(mappedBy = "agente", cascade = CascadeType.ALL)
    private List<Lead> leads;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Como todos los agentes son iguales, les damos a todos el mismo permiso.
        return List.of(new SimpleGrantedAuthority("ROLE_AGENTE"));
    }

    @Override
    public String getUsername() {
        return this.email; // El email será el login (Mateo@leadera.com)
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    // Por ahora, mantenemos la cuenta siempre activa
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }


}
