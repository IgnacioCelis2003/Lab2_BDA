import { findElevationAtPoint } from "@esri/arcgis-rest-elevation";
import { ApiKeyManager } from "@esri/arcgis-rest-request";

const authentication = ApiKeyManager.fromKey(
  "AAPTxy8BH1VEsoebNVZXo8HurHnGHIMDvj-hrFPsJJdlTmRpjUyXUiSefiaxrXphqwYz7kuRATMU425FS1CPNob_xyrfMbh1cSUc6oxcQJ18t1sC7h1upQNz02doAmVsLAgycF-ENPBm45NEso4cyueOwmFthetz24by4TuxPC1wVTzssCv3DxL_HLPE5B4iI9vgGJqnFIljhD-m838RTdyc1uTqhvyvhVVKYGS7VfzYUx4.AT1_apYfVUmB"
);

export default defineEventHandler(async (event) => {
  const lon = parseFloat(getRouterParam(event, "lon") || "");
  const lat = parseFloat(getRouterParam(event, "lat") || "");

  if (isNaN(lon) || isNaN(lat)) {
    throw createError({
      statusCode: 400,
      message: "Coordenadas inválidas. Se requiere lon y lat numéricos.",
    });
  }

  try {
    const response = await findElevationAtPoint({
      lon,
      lat,
      authentication,
    });

    return {
      lon,
      lat,
      elevation: response.result.point.z, // metros sobre el nivel del mar
    };
  } catch (error: any) {
    console.error("Error obteniendo elevación:", error);
    throw createError({
      statusCode: 500,
      message: error.message || "Error al obtener elevación desde Esri",
    });
  }
});
