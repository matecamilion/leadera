package com.leadera.leadera.dto;

import com.leadera.leadera.entity.Lead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadsHoyResponse {

    private List<Lead> nuevosSinContacto;
    private List<Lead> prioritarios;
    private List<Lead> seguimientosDeHoy;

    private List<Lead> contactadosHoy;

    private int totalTareasDelDia;
    private int tareasCompletadasDelDia;

}
