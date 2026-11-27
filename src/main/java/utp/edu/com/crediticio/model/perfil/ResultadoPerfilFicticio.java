package utp.edu.com.crediticio.model.perfil;

/** Resultado informativo de la consulta; nunca modifica una decisión crediticia. */
public record ResultadoPerfilFicticio(
        boolean consultaAplicable,
        PerfilFicticioAcademico perfil,
        String mensaje) {

    public static ResultadoPerfilFicticio noAplica() {
        return new ResultadoPerfilFicticio(false, null, null);
    }

    public static ResultadoPerfilFicticio disponible(PerfilFicticioAcademico perfil) {
        return new ResultadoPerfilFicticio(true, perfil, null);
    }

    public static ResultadoPerfilFicticio noEncontrado() {
        return new ResultadoPerfilFicticio(true, null,
                "No existe un perfil ficticio académico para este DNI.");
    }

    public static ResultadoPerfilFicticio noDisponible() {
        return new ResultadoPerfilFicticio(true, null,
                "El perfil ficticio académico no está disponible en este momento.");
    }
}
