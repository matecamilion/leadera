export interface Busqueda {
    id?: number;
    precioMin?: number;
    precioMax?: number;
    cantidadAmbientes?: number;
    metrosTotales?: number;
    metrosCubiertos?: number;
    metrosDescubiertos?: number;
    tipoVivienda?: string;
    zona?: string;
    observaciones?: string;
}
