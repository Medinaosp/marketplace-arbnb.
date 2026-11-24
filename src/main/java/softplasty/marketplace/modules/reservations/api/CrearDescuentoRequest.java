package softplasty.marketplace.modules.reservations.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearDescuentoRequest(@NotBlank @Size(max=40) String codigo,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal porcentaje,
        @Positive int nochesMinimas, @NotNull LocalDate vigenteDesde, @NotNull LocalDate vigenteHasta) { }
