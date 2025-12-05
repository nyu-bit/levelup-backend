# Level-Up Gamer Backend

Backend Spring Boot 3.2 para el e-commerce "Level-Up Gamer" - Tienda de videojuegos.

## 🚀 Tecnologías

- Java 17
- Spring Boot 3.2
- Spring Security 6 + JWT
- MySQL (producción) / H2 (desarrollo)
- Gradle 8.5
- Swagger/OpenAPI

## 📋 Endpoints Principales

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | Registro de usuarios |
| `/api/v1/auth/login` | POST | Login (devuelve JWT) |
| `/api/v1/products` | GET | Listar productos |
| `/api/v1/sales/checkout` | POST | Crear venta con pago |
| `/api/v1/sales/transbank/init` | POST | Iniciar transacción Transbank |
| `/api/v1/sales/transbank/callback` | POST | Callback de Transbank |
| `/api/v1/sales/payment-status/{token}` | GET | Consultar estado de pago |

## 🔧 Configuración de Perfiles

El proyecto soporta múltiples ambientes mediante perfiles de Spring:

### Perfiles Disponibles

| Perfil | Archivo | Uso |
|--------|---------|-----|
| (default) | `application.properties` | Configuración base |
| `dev` | `application-dev.properties` | Desarrollo local con H2 |
| `ec2` | `application-ec2.properties` | Instancia AWS EC2 |
| `prod` | `application-prod.properties` | Producción |

### Activar un Perfil

```bash
# Opción 1: Variable de entorno
export SPRING_PROFILES_ACTIVE=ec2
java -jar levelup-backend.jar

# Opción 2: Argumento JVM
java -jar -Dspring.profiles.active=ec2 levelup-backend.jar

# Opción 3: En application.properties
spring.profiles.active=ec2
```

---

## 🏧 Configuración de URL de API y Transbank Mock (IP EC2 / Dominio)

### Propiedades Principales

Las URLs del mock de Transbank y del backend se configuran en los archivos de propiedades:

#### 1. URL Base del Backend (`app.base-url`)

```properties
# application.properties o application-{perfil}.properties
app.base-url=https://api.levelupgamer.lol
```

**¿Cuándo cambiarla?**
- Cuando cambie la IP de la instancia EC2
- Cuando se configure un dominio personalizado
- Cuando se migre a otro servidor

Esta URL se usa para construir las URLs de callback que Transbank utiliza para notificar el resultado del pago.

#### 2. URL del Mock de Transbank (`payment.transbank.mock-base-url`)

```properties
# URL base del mock
payment.transbank.mock-base-url=https://webpay.mock/api

# Endpoints derivados (se construyen automáticamente)
payment.transbank.mock-init-endpoint=${payment.transbank.mock-base-url}/transaction
payment.transbank.mock-status-endpoint=${payment.transbank.mock-base-url}/status
payment.transbank.mock-confirm-endpoint=${payment.transbank.mock-base-url}/confirm
```

**¿Cuándo cambiarla?**
- Cuando el mock de Transbank cambie de URL
- Cuando se pase a integración con Transbank real
- Para usar diferentes mocks según el ambiente

#### 3. URLs de Retorno y Callback

```properties
# URL donde se redirige al usuario después del pago (frontend)
payment.transbank.return-url=https://levelupgamer.lol/pago/retorno

# URL donde Transbank notifica el resultado (backend)
payment.transbank.callback-url=${app.base-url}/api/v1/sales/transbank/callback
```

### Configuración por Ambiente

#### Desarrollo Local (`application-dev.properties`)
```properties
app.base-url=http://localhost:8080
payment.transbank.mock-base-url=http://localhost:8081/mock/transbank
payment.transbank.return-url=http://localhost:5173/pago/retorno
```

#### EC2 (`application-ec2.properties`)
```properties
app.base-url=https://api.levelupgamer.lol
payment.transbank.mock-base-url=https://webpay.mock/api
payment.transbank.return-url=https://levelupgamer.lol/pago/retorno
```

#### Producción (`application-prod.properties`)
```properties
app.base-url=${APP_BASE_URL:https://api.levelupgamer.lol}
payment.transbank.mock-base-url=${TRANSBANK_BASE_URL:https://webpay3gint.transbank.cl}
```

### ⚠️ Qué Hacer Cuando Cambie la IP de EC2

1. **Actualizar `app.base-url`** en `application-ec2.properties`:
   ```properties
   app.base-url=http://NUEVA_IP:8080
   # o si tienes dominio:
   app.base-url=https://api.tudominio.com
   ```

2. **Recompilar** (si es necesario):
   ```bash
   ./gradlew clean build -x test
   ```

3. **Reiniciar el servicio**:
   ```bash
   sudo systemctl restart levelup
   # o
   sudo systemctl stop levelup
   java -jar -Dspring.profiles.active=ec2 levelup-backend.jar
   ```

4. **Verificar** que los endpoints funcionan:
   ```bash
   curl https://api.tudominio.com/api/v1/products
   ```

---

## 🔐 Variables de Entorno (Producción)

Para mayor seguridad, en producción usa variables de entorno:

```bash
export DB_URL=jdbc:mysql://localhost:3306/levelup_db
export DB_USERNAME=levelup
export DB_PASSWORD=tu_password_seguro
export APP_BASE_URL=https://api.levelupgamer.lol
export TRANSBANK_BASE_URL=https://webpay3gint.transbank.cl
export TRANSBANK_COMMERCE_CODE=597055555532
export TRANSBANK_API_KEY=tu_api_key
```

---

## 🏃 Ejecución

### Desarrollo (Codespaces/Local)
```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

### EC2
```bash
java -jar -Dspring.profiles.active=ec2 levelup-backend-0.0.1-SNAPSHOT.jar
```

### Producción
```bash
java -jar -Dspring.profiles.active=prod levelup-backend-0.0.1-SNAPSHOT.jar
```

---

## 📦 Releases

Los releases se publican en GitHub:
https://github.com/nyu-bit/levelup-backend/releases

### Desplegar en EC2
```bash
cd /opt/levelup
sudo systemctl stop levelup
wget https://github.com/nyu-bit/levelup-backend/releases/download/vX.X.X/levelup-backend-0.0.1-SNAPSHOT.jar -O levelup-backend.jar
sudo systemctl start levelup
```

---

## 📝 Notas

- El CORS está configurado y manejado por Nginx (no por Spring)
- El mock de Transbank es simulado; para producción real, usar SDK oficial
- Los timeouts de conexión son configurables por ambiente