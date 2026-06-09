package com.leadera.leadera.enums;

// DUENO sin Ñ a propósito: el valor viaja en el JWT, en la DB y en los
// @PreAuthorize("hasRole('DUENO')"); un carácter no-ASCII ahí es fuente de bugs.
public enum RolAgente {
    DUENO,
    AGENTE
}
