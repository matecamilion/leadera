package com.leadera.leadera.service;

import com.leadera.leadera.dto.DashboardDTO;
import com.leadera.leadera.dto.DashboardDTO.EvolucionDTO;
import com.leadera.leadera.dto.DashboardDTO.FunnelDTO;
import com.leadera.leadera.dto.DashboardDTO.KpiDTO;
import com.leadera.leadera.dto.DashboardDTO.KpisDTO;
import com.leadera.leadera.dto.DashboardDTO.OrigenDTO;
import com.leadera.leadera.dto.DashboardDTO.SnapshotDTO;
import com.leadera.leadera.entity.Agente;
import com.leadera.leadera.entity.Lead;
import com.leadera.leadera.enums.EstadoLead;
import com.leadera.leadera.enums.EstadoOperacion;
import com.leadera.leadera.exception.ResourceNotFoundException;
import com.leadera.leadera.repository.AgenteRepository;
import com.leadera.leadera.repository.InteraccionRepository;
import com.leadera.leadera.repository.LeadRepository;
import com.leadera.leadera.repository.OperacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LeadRepository leadRepository;
    private final OperacionRepository operacionRepository;
    private final InteraccionRepository interaccionRepository;
    private final AgenteRepository agenteRepository;

    public DashboardService(LeadRepository leadRepository,
                            OperacionRepository operacionRepository,
                            InteraccionRepository interaccionRepository,
                            AgenteRepository agenteRepository) {
        this.leadRepository = leadRepository;
        this.operacionRepository = operacionRepository;
        this.interaccionRepository = interaccionRepository;
        this.agenteRepository = agenteRepository;
    }

    public DashboardDTO obtenerDashboard(Long agenteId, String periodo) {
        Agente agente = agenteRepository.findById(agenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        int dias = resolverDias(periodo);
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioPeriodo = hoy.minusDays(dias - 1L).atStartOfDay();
        LocalDateTime finPeriodo = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioAnterior = inicioPeriodo.minusDays(dias);
        LocalDateTime finAnterior = inicioPeriodo;

        SnapshotDTO snapshot = construirSnapshot(agenteId, agente, hoy);
        EvolucionDTO evolucion = construirEvolucion(agenteId, hoy, dias, inicioPeriodo, finPeriodo);
        KpisDTO kpis = construirKpis(agenteId, snapshot.activos(), evolucion,
                inicioPeriodo, finPeriodo, inicioAnterior, finAnterior);
        List<OrigenDTO> origenes = obtenerOrigenes(agenteId, inicioPeriodo, finPeriodo);

        return new DashboardDTO(periodo, dias, snapshot, kpis, evolucion, origenes);
    }

    private int resolverDias(String periodo) {
        if (periodo == null) return 30;
        return switch (periodo.toLowerCase()) {
            case "7d" -> 7;
            case "90d" -> 90;
            case "ano", "año", "1a", "365d" -> 365;
            default -> 30;
        };
    }

    private SnapshotDTO construirSnapshot(Long agenteId, Agente agente, LocalDate hoy) {
        long activos = leadRepository.countByAgenteIdAndEstadoNot(agenteId, EstadoLead.INACTIVO);
        long calientes = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.CALIENTE);
        long tibios = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.TIBIO);
        long frios = leadRepository.countByAgenteIdAndEstado(agenteId, EstadoLead.FRIO);
        long ganadosMes = operacionRepository.countOperacionesGanadasDelMes(agenteId);

        int meta = agente.getMetaMensualCierres() == null ? 12 : agente.getMetaMensualCierres();
        int diasRestantes = hoy.lengthOfMonth() - hoy.getDayOfMonth();

        FunnelDTO funnel = construirFunnel(agenteId, ganadosMes);

        return new SnapshotDTO(activos, calientes, tibios, frios, ganadosMes,
                meta, diasRestantes, funnel);
    }

    private FunnelDTO construirFunnel(Long agenteId, long ganadosMes) {
        long contactados = leadRepository.countContactados(agenteId);
        long calificados = leadRepository.countCalificados(agenteId);
        long visita = operacionRepository.countLeadsConVisita(agenteId);
        long oferta = operacionRepository.countLeadsDistintosConOperacionEnEstados(
                agenteId,
                List.of(EstadoOperacion.PUBLICADA, EstadoOperacion.RESERVADA, EstadoOperacion.EN_NEGOCIACION));
        long cerrado = operacionRepository.countLeadsDistintosConOperacionEnEstados(
                agenteId,
                List.of(EstadoOperacion.CERRADA_GANADA));
        return new FunnelDTO(contactados, calificados, visita, oferta, cerrado);
    }

    private EvolucionDTO construirEvolucion(Long agenteId, LocalDate hoy, int dias,
                                             LocalDateTime inicio, LocalDateTime fin) {
        LocalDate inicioDate = hoy.minusDays(dias - 1L);

        List<LocalDateTime> ingresos = leadRepository.findFechasIngresoEnRango(agenteId, inicio, fin);
        List<LocalDateTime> ganados = operacionRepository.findFechasCierreEnRango(
                agenteId, EstadoOperacion.CERRADA_GANADA, inicio, fin);
        List<LocalDateTime> perdidos = operacionRepository.findFechasCierreEnRango(
                agenteId, EstadoOperacion.CANCELADA, inicio, fin);

        long[] nuevosArr = agruparPorDia(ingresos, inicioDate, dias);
        long[] ganadosArr = agruparPorDia(ganados, inicioDate, dias);
        long[] perdidosArr = agruparPorDia(perdidos, inicioDate, dias);

        List<String> fechas = new ArrayList<>(dias);
        List<Long> nuevosL = new ArrayList<>(dias);
        List<Long> ganadosL = new ArrayList<>(dias);
        List<Long> perdidosL = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            fechas.add(inicioDate.plusDays(i).format(FMT_FECHA));
            nuevosL.add(nuevosArr[i]);
            ganadosL.add(ganadosArr[i]);
            perdidosL.add(perdidosArr[i]);
        }
        return new EvolucionDTO(fechas, nuevosL, ganadosL, perdidosL);
    }

    private long[] agruparPorDia(List<LocalDateTime> fechas, LocalDate inicio, int dias) {
        long[] arr = new long[dias];
        for (LocalDateTime f : fechas) {
            if (f == null) continue;
            int idx = (int) ChronoUnit.DAYS.between(inicio, f.toLocalDate());
            if (idx >= 0 && idx < dias) arr[idx]++;
        }
        return arr;
    }

    private KpisDTO construirKpis(Long agenteId,
                                  long activosSnapshot,
                                  EvolucionDTO ev,
                                  LocalDateTime inicioActual, LocalDateTime finActual,
                                  LocalDateTime inicioAnt, LocalDateTime finAnt) {
        long nuevosActual = sum(ev.nuevos());
        long ganadosActual = sum(ev.ganados());

        long nuevosAnt = leadRepository
                .countByAgenteIdAndFechaEntradaGreaterThanEqualAndFechaEntradaLessThan(
                        agenteId, inicioAnt, finAnt);
        long ganadosAnt = operacionRepository.countCierresEnRango(
                agenteId, EstadoOperacion.CERRADA_GANADA, inicioAnt, finAnt);

        double tasaConvActual = nuevosActual > 0
                ? round1((double) ganadosActual / nuevosActual * 100.0)
                : 0.0;
        double tasaConvAnt = nuevosAnt > 0
                ? round1((double) ganadosAnt / nuevosAnt * 100.0)
                : 0.0;

        double tiempoRespActual = round1(tiempoRespuestaPromedioDias(agenteId, inicioActual, finActual));
        double tiempoRespAnt = round1(tiempoRespuestaPromedioDias(agenteId, inicioAnt, finAnt));

        List<Double> sparkActivos = acumular(ev.nuevos());
        List<Double> sparkGanados = acumular(ev.ganados());
        List<Double> sparkTasa = acumularRatio(ev.ganados(), ev.nuevos());
        List<Double> sparkTiempo = serieDoubleConstante(tiempoRespActual, ev.fechas().size());

        KpiDTO leadsActivos = new KpiDTO(
                activosSnapshot,
                activosSnapshot,
                deltaPct(nuevosActual, nuevosAnt),
                direccion(nuevosActual, nuevosAnt, false),
                sparkActivos);

        KpiDTO tasaConversion = new KpiDTO(
                tasaConvActual,
                tasaConvAnt,
                deltaPct(tasaConvActual, tasaConvAnt),
                direccion(tasaConvActual, tasaConvAnt, false),
                sparkTasa);

        KpiDTO tiempoRespuesta = new KpiDTO(
                tiempoRespActual,
                tiempoRespAnt,
                deltaPct(tiempoRespActual, tiempoRespAnt),
                direccion(tiempoRespActual, tiempoRespAnt, true),
                sparkTiempo);

        KpiDTO ganados = new KpiDTO(
                ganadosActual,
                ganadosAnt,
                deltaPct(ganadosActual, ganadosAnt),
                direccion(ganadosActual, ganadosAnt, false),
                sparkGanados);

        return new KpisDTO(leadsActivos, tasaConversion, tiempoRespuesta, ganados);
    }

    private double tiempoRespuestaPromedioDias(Long agenteId, LocalDateTime inicio, LocalDateTime fin) {
        List<Lead> leads = leadRepository.findLeadsConFechaEntrada(agenteId);
        return leads.stream()
                .filter(l -> l.getFechaEntrada() != null
                        && !l.getFechaEntrada().isBefore(inicio)
                        && l.getFechaEntrada().isBefore(fin))
                .mapToLong(lead -> {
                    LocalDateTime primera = interaccionRepository.findPrimeraInteraccion(lead.getId());
                    if (primera == null) return -1L;
                    return ChronoUnit.HOURS.between(lead.getFechaEntrada(), primera);
                })
                .filter(h -> h >= 0)
                .average()
                .orElse(0.0) / 24.0;
    }

    private List<OrigenDTO> obtenerOrigenes(Long agenteId, LocalDateTime inicio, LocalDateTime fin) {
        List<Object[]> rows = leadRepository.contarOrigenesEnRango(agenteId, inicio, fin);
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<OrigenDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String nombre = (String) r[0];
            long cant = ((Number) r[1]).longValue();
            double porc = total > 0 ? round1((double) cant / total * 100.0) : 0.0;
            out.add(new OrigenDTO(nombre, cant, porc));
        }
        return out;
    }

    // ---- helpers ----

    private long sum(List<Long> l) {
        long s = 0;
        for (Long x : l) s += x == null ? 0 : x;
        return s;
    }

    private List<Double> acumular(List<Long> serie) {
        List<Double> out = new ArrayList<>(serie.size());
        double acc = 0;
        for (Long v : serie) {
            acc += v == null ? 0 : v;
            out.add(acc);
        }
        return out;
    }

    private List<Double> acumularRatio(List<Long> num, List<Long> den) {
        List<Double> out = new ArrayList<>(num.size());
        double accN = 0, accD = 0;
        for (int i = 0; i < num.size(); i++) {
            accN += num.get(i) == null ? 0 : num.get(i);
            accD += den.get(i) == null ? 0 : den.get(i);
            out.add(accD > 0 ? round1(accN / accD * 100.0) : 0.0);
        }
        return out;
    }

    private List<Double> serieDoubleConstante(double v, int n) {
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(v);
        return out;
    }

    private double deltaPct(double actual, double anterior) {
        if (anterior == 0) {
            if (actual == 0) return 0.0;
            return 100.0;
        }
        return round1((actual - anterior) / anterior * 100.0);
    }

    /**
     * direccion: "up", "down", "flat". Si invertido=true, menor es mejor (ej: tiempo respuesta).
     */
    private String direccion(double actual, double anterior, boolean invertido) {
        if (actual == anterior) return "flat";
        boolean sube = actual > anterior;
        if (invertido) return sube ? "down" : "up";
        return sube ? "up" : "down";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
