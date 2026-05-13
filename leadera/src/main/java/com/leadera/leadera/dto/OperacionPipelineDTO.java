package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Operacion;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.enums.TipoOperacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperacionPipelineDTO {

    private Long id;
    private String titulo;
    private EstadoOperacion estado;
    private TipoOperacion tipoOperacion;
    private Long leadId;
    private String nombreLead;
    private String apellidoLead;
    private String descripcionPropiedad;
    private String precioEstimado;
    private LocalDateTime fechaCierre;

    public static OperacionPipelineDTO fromEntity(Operacion operacion) {
        OperacionPipelineDTO dto = new OperacionPipelineDTO();
        dto.setId(operacion.getId());
        dto.setTitulo(operacion.getTitulo());
        dto.setEstado(operacion.getEstadoOperacion());
        dto.setTipoOperacion(operacion.getTipoOperacion());

        if (operacion.getLead() != null) {
            dto.setLeadId(operacion.getLead().getId());
            dto.setNombreLead(operacion.getLead().getNombre());
            dto.setApellidoLead(operacion.getLead().getApellido());
        }

        dto.setDescripcionPropiedad(armarDescripcion(operacion));
        dto.setPrecioEstimado(calcularPrecioEstimado(operacion));
        dto.setFechaCierre(operacion.getFechaCierre());
        return dto;
    }

    private static String calcularPrecioEstimado(Operacion op) {
        if (op.getMontoOperacion() != null) {
            return "USD " + formatear(op.getMontoOperacion()) + " (acordado)";
        }
        TipoOperacion tipo = op.getTipoOperacion();
        if (tipo == TipoOperacion.VENTA && op.getPropiedad() != null && op.getPropiedad().getPrecio() != null) {
            return "USD " + formatear(op.getPropiedad().getPrecio());
        }
        if (tipo == TipoOperacion.COMPRA && op.getBusqueda() != null) {
            BigDecimal min = op.getBusqueda().getPrecioMin();
            BigDecimal max = op.getBusqueda().getPrecioMax();
            if (min != null && max != null) {
                return "USD " + formatear(min) + " - " + formatear(max);
            }
            if (min != null) return "Desde USD " + formatear(min);
            if (max != null) return "Hasta USD " + formatear(max);
            return "Sin presupuesto registrado";
        }
        return "Sin precio registrado";
    }

    private static String formatear(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String armarDescripcion(Operacion operacion) {
        if (operacion.getPropiedad() != null) {
            String direccion = operacion.getPropiedad().getDireccion();
            String zona = operacion.getPropiedad().getZona();
            if (direccion != null && zona != null) {
                return direccion + " (" + zona + ")";
            }
            return direccion != null ? direccion : zona;
        }
        if (operacion.getBusqueda() != null) {
            String zona = operacion.getBusqueda().getZona();
            String tipo = operacion.getBusqueda().getTipoVivienda() != null
                    ? operacion.getBusqueda().getTipoVivienda().name()
                    : null;
            StringBuilder sb = new StringBuilder("Búsqueda");
            if (tipo != null) sb.append(" de ").append(tipo);
            if (zona != null) sb.append(" en ").append(zona);
            return sb.toString();
        }
        return "Sin propiedad ni búsqueda asociada";
    }
}
