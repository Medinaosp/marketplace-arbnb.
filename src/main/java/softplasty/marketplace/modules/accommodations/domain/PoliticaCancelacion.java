package softplasty.marketplace.modules.accommodations.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "politica_cancelacion")
public class PoliticaCancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reglas", nullable = false, columnDefinition = "TEXT[]")
    private String[] reglas = new String[0];

    protected PoliticaCancelacion() {
    }

    public PoliticaCancelacion(String nombre, String[] reglas) {
        this.nombre = nombre;
        this.reglas = reglas.clone();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String[] getReglas() { return reglas.clone(); }
}
