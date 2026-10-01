package service;

import model.TelemetryData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesador de mensajes del protocolo de telemetría IoT sobre UDP.
 * 
 * Formato de mensajes recibidos:
 *   - Registro de telemetría: "DEVICE_ID;SENSOR_TYPE;VALUE" (ej. "sensor-01;TEMP;25.5")
 *   - Consulta de estado:      "STATUS;DEVICE_ID" (ej. "STATUS;sensor-01")
 */
public class TelemetryProcessor {

    private final Map<String, TelemetryData> lastReadings = new ConcurrentHashMap<>();

    /**
     * Procesa un mensaje de texto recibido por UDP y devuelve la respuesta
     * correspondiente según las reglas del protocolo de telemetría.
     * 
     * @param rawMessage Mensaje en texto plano recibido en el datagrama UDP.
     * @return Respuesta que será enviada de regreso al cliente emisor.
     */
    public String process(String rawMessage) {
        // TODO Paso 1.1: Validar que el mensaje no sea nulo ni esté vacío (usar trim()).
        // Si no es válido, retornar "ERROR;INVALID_FORMAT".

        // TODO Paso 1.2: Separar el mensaje usando el delimitador ";".
        // Si el arreglo resultante está vacío, retornar "ERROR;INVALID_FORMAT".

        // TODO Paso 1.3: Si la primera parte es "STATUS" (ignorar mayúsculas/minúsculas):
        //   - Validar que tenga exactamente 2 partes y que el DEVICE_ID no esté en blanco.
        //   - Si no cumple, retornar "ERROR;INVALID_FORMAT".
        //   - Buscar en 'lastReadings' por DEVICE_ID.
        //   - Si no existe, retornar "ERROR;DEVICE_NOT_FOUND".
        //   - Si existe, retornar "STATUS_OK;DEVICE_ID;SENSOR_TYPE;VALUE".

        // TODO Paso 1.4: Validar formato de telemetría: deben ser exactamente 3 partes no vacías:
        // [0] = deviceId, [1] = sensorType, [2] = valueStr.
        // Si no cumple, retornar "ERROR;INVALID_FORMAT".
        // Intentar convertir valueStr a double (Double.parseDouble).
        // Si falla con NumberFormatException, retornar "ERROR;INVALID_FORMAT".

        // TODO Paso 1.5: Guardar la lectura válida en 'lastReadings':
        // lastReadings.put(deviceId, new TelemetryData(deviceId, sensorType, value));

        // TODO Paso 1.6: Validar sensorType (TEMP, HUMIDITY, BATTERY) y evaluar rangos:
        // - TEMP:
        //     valor > 40.0 -> "ALERT;HIGH_TEMPERATURE;" + value
        //     valor < 0.0  -> "ALERT;FREEZING_TEMPERATURE;" + value
        //     otro         -> "OK;TEMP_RECORDED;" + value
        // - HUMIDITY:
        //     valor > 90.0 -> "ALERT;HIGH_HUMIDITY;" + value
        //     valor < 20.0 -> "ALERT;LOW_HUMIDITY;" + value
        //     otro         -> "OK;HUMIDITY_RECORDED;" + value
        // - BATTERY:
        //     valor < 20.0 -> "ALERT;LOW_BATTERY;" + value
        //     otro         -> "OK;BATTERY_OK;" + value
        // - Cualquier otro sensorType:
        //     retornar "ERROR;UNKNOWN_SENSOR_TYPE"

        // return "ERROR;NOT_IMPLEMENTED"; // Reemplazar con su implementación

        // Paso 1.1:
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        // Paso 1.2:
        String[] messageParts = rawMessage.trim().split(";", -1);

        // Paso 1.3:
        if (messageParts[0].trim().equalsIgnoreCase("STATUS")) {

            if (messageParts.length != 2 || messageParts[1].trim().isEmpty()) {
                return "ERROR;INVALID_FORMAT";
            }

            String deviceId = messageParts[1].trim();
            TelemetryData data = lastReadings.get(deviceId);

            if (data == null) {
                return "ERROR;DEVICE_NOT_FOUND";
            }

            return "STATUS_OK;" + data.getDeviceId() + ";" + data.getSensorType() + ";" + data.getValue();
        }

        // Paso 1.4:
        if (messageParts.length != 3) {
            return "ERROR;INVALID_FORMAT";
        }

        if (messageParts[0].trim().isEmpty() || messageParts[1].trim().isEmpty() || messageParts[2].trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        String deviceId = messageParts[0].trim();
        String sensorType = messageParts[1].trim();
        String valueStr = messageParts[2].trim();

        double value;

        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return "ERROR;INVALID_FORMAT";
        }

        // Paso 1.6:
        String response;

        switch (sensorType) {
            case "TEMP":
                if (value > 40.0) {
                    response = "ALERT;HIGH_TEMPERATURE;" + value;
                } else if (value < 0.0) {
                    response = "ALERT;FREEZING_TEMPERATURE;" + value;
                } else {
                    response = "OK;TEMP_RECORDED;" + value;
                }
                break;

            case "HUMIDITY":
                if (value > 90.0) {
                    response = "ALERT;HIGH_HUMIDITY;" + value;
                } else if (value < 20.0) {
                    response = "ALERT;LOW_HUMIDITY;" + value;
                } else {
                    response = "OK;HUMIDITY_RECORDED;" + value;
                }
                break;

            case "BATTERY":
                if (value < 20.0) {
                    response = "ALERT;LOW_BATTERY;" + value;
                } else {
                    response = "OK;BATTERY_OK;" + value;
                }
                break;

            default:
                return "ERROR;UNKNOWN_SENSOR_TYPE";
        }

        // Paso 1.5:
        lastReadings.put(deviceId,new TelemetryData(deviceId, sensorType, value));

        return response;

    }

    public Map<String, TelemetryData> getLastReadings() {
        return lastReadings;
    }

    public void clear() {
        lastReadings.clear();
    }
}
