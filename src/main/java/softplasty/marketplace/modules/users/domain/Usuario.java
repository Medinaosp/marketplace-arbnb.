package softplasty.marketplace.modules.users.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "hash_contrasena", nullable = false)
    private String hashContrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolUsuario rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCuenta estado;

    @Column(name = "creado_en", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant creadoEn;

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, String hashContrasena, RolUsuario rol,
                   EstadoCuenta estado, Instant creadoEn) {
        this.nombre = nombre;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getHashContrasena() { return hashContrasena; }
    public RolUsuario getRol() { return rol; }
    public EstadoCuenta getEstado() { return estado; }
    public Instant getCreadoEn() { return creadoEn; }

    public void actualizarPerfil(String nombre, String correo) {
        this.nombre = nombre;
        this.correo = correo;
    }

    public void cambiarHashContrasena(String nuevoHash) {
        this.hashContrasena = nuevoHash;
    }

    public void desactivar() {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("Solo una cuenta activa puede desactivarse por su titular.");
        }
        estado = EstadoCuenta.DESACTIVADA;
    }

    public void restringir() {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("Solo una cuenta activa puede restringirse.");
        }
        estado = EstadoCuenta.RESTRINGIDA;
    }

    public void activar() {
        if (estado == EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("La cuenta ya está activa.");
        }
        estado = EstadoCuenta.ACTIVA;
    }
}
