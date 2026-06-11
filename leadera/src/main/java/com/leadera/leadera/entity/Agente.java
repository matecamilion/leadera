package com.leadera.leadera.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leadera.leadera.enums.RolAgente;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
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

    @Column(name = "meta_mensual_cierres", nullable = false)
    @ColumnDefault("12")
    private Integer metaMensualCierres = 12;

    // JsonIgnore: la entidad Agente se serializa dentro de Lead (vía
    // @JsonIgnoreProperties en Lead.agente) y la inmobiliaria no debe
    // viajar en esos payloads. Para exponerla siempre usar DTOs.
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "inmobiliaria_id", nullable = false)
    private Inmobiliaria inmobiliaria;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolAgente rol;

    // Supervisor del agente. Solo lo usa ASISTENTE, que apunta al AGENTE
    // bajo el que trabaja; DUENO y AGENTE quedan en null.
    // Exclusiones obligatorias: Agente usa @Data y esto es un self-FK; sin
    // ellas habría recursión infinita en toString()/equals()/hashCode().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Agente supervisor;

    @Column(name = "debe_cambiar_password", nullable = false)
    @ColumnDefault("false")
    private boolean debeCambiarPassword = false;

    @Column(name = "activo", nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

    @ToString.Exclude
    @OneToMany(mappedBy = "agente", cascade = CascadeType.ALL)
    private List<Lead> leads;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Defensa ante entidades aún no persistidas con la migración corrida:
        // sin rol explícito se asume AGENTE (el permiso más bajo).
        RolAgente rolEfectivo = (rol != null) ? rol : RolAgente.AGENTE;
        return List.of(new SimpleGrantedAuthority("ROLE_" + rolEfectivo.name()));
    }

    @Override
    public String getUsername() {
        return this.email; // El email será el login (Mateo@leadera.com)
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    // Un agente desactivado por el dueño no puede loguearse:
    // DaoAuthenticationProvider lanza DisabledException si isEnabled() es false.
    @Override public boolean isEnabled() { return this.activo; }


}
