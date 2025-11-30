# Level-Up Gamer Backend

Backend API REST para la tienda de videojuegos **Level-Up Gamer**, desarrollado con Spring Boot 3.2, Spring Security, JWT y H2 Database.

## 📋 Requisitos

- Java 17+
- Gradle 8.x (incluido wrapper)

## 🚀 Ejecutar el proyecto

```bash
cd levelup-backend

# Dar permisos al wrapper de Gradle (Linux/Mac)
chmod +x gradlew

# Compilar el proyecto
./gradlew clean build -x test

# Ejecutar la aplicación
./gradlew bootRun
```

La aplicación estará disponible en: `http://localhost:8080`

## 📚 Documentación API (Swagger)

Una vez ejecutando la aplicación:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## 🗄️ Base de Datos H2

Consola H2 disponible en: http://localhost:8080/h2-console

- **JDBC URL:** `jdbc:h2:mem:levelupdb`
- **Usuario:** `sa`
- **Password:** *(vacío)*

## 👤 Usuarios de Prueba

| Email | Password | Roles |
|-------|----------|-------|
| admin@levelup.cl | admin123 | ADMIN, VENDEDOR, CLIENTE |
| vendedor@levelup.cl | vendedor123 | VENDEDOR, CLIENTE |
| cliente@levelup.cl | cliente123 | CLIENTE |

## 🔗 Endpoints API

### Autenticación (`/api/v1/auth`)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/register` | Registrar nuevo usuario | No |
| POST | `/login` | Iniciar sesión | No |

### Usuarios (`/api/v1/users`)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/me` | Obtener usuario actual | Sí |
| PATCH | `/me` | Actualizar usuario actual | Sí |

### Productos (`/api/v1/products`)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/` | Listar productos (paginado + filtros) | No |
| GET | `/{id}` | Obtener producto por ID | No |
| POST | `/` | Crear producto | ADMIN |
| PUT | `/{id}` | Actualizar producto | ADMIN |
| PATCH | `/{id}/stock` | Actualizar stock | ADMIN, VENDEDOR |
| DELETE | `/{id}` | Eliminar producto | ADMIN |

**Parámetros de filtro:**
- `page` (int, default: 0)
- `size` (int, default: 12)
- `category` (string)
- `brand` (string)
- `minPrice` (int)
- `maxPrice` (int)
- `featured` (boolean)
- `isOffer` (boolean)
- `sortBy` (string, default: "id")
- `sortDir` (string: "asc" | "desc")

### Ventas (`/api/v1/sales`)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/` | Crear venta | CLIENTE |
| GET | `/` | Mis ventas | CLIENTE |
| GET | `/{id}` | Detalle de venta | CLIENTE (propia), ADMIN/VENDEDOR (todas) |
| GET | `/all` | Todas las ventas (paginado) | ADMIN, VENDEDOR |
| POST | `/transbank/callback` | Callback de Transbank | No |

## 🧪 Ejemplos de Uso

### Registrar Usuario

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "password": "password123",
    "confirmPassword": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cliente@levelup.cl",
    "password": "cliente123"
  }'
```

Respuesta:
```json
{
  "token": "Bearer eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 3,
    "name": "Cliente Demo",
    "email": "cliente@levelup.cl",
    "roles": ["CLIENTE"]
  }
}
```

### Obtener Productos con Filtros

```bash
# Todos los productos
curl http://localhost:8080/api/v1/products

# Filtrar por categoría
curl "http://localhost:8080/api/v1/products?category=Consolas"

# Filtrar por precio
curl "http://localhost:8080/api/v1/products?minPrice=50000&maxPrice=100000"

# Productos destacados
curl "http://localhost:8080/api/v1/products?featured=true"

# Ofertas paginadas
curl "http://localhost:8080/api/v1/products?isOffer=true&page=0&size=5"
```

### Crear Venta

```bash
curl -X POST http://localhost:8080/api/v1/sales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "items": [
      { "productId": 1, "quantity": 1 },
      { "productId": 4, "quantity": 2 }
    ]
  }'
```

### Callback de Transbank

```bash
# Aprobar venta
curl -X POST "http://localhost:8080/api/v1/sales/transbank/callback?token=<TRANSBANK_TOKEN>&status=OK"

# Rechazar venta
curl -X POST "http://localhost:8080/api/v1/sales/transbank/callback?token=<TRANSBANK_TOKEN>&status=FAILED"
```

## 📁 Estructura del Proyecto

```
src/main/java/com/levelup/backend/
├── LevelUpBackendApplication.java    # Clase principal
├── config/
│   ├── DataInitializer.java          # Datos iniciales
│   ├── GlobalExceptionHandler.java   # Manejo de errores
│   ├── SecurityConfig.java           # Configuración de seguridad
│   └── SwaggerConfig.java            # Configuración de OpenAPI
├── dto/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── SaleItemRequest.java
│   ├── SaleRequest.java
│   ├── UserResponse.java
│   └── UserUpdateRequest.java
├── product/
│   ├── Product.java                  # Entidad
│   ├── ProductController.java        # Controlador REST
│   ├── ProductRepository.java        # Repositorio JPA
│   └── ProductService.java           # Lógica de negocio
├── sale/
│   ├── Sale.java                     # Entidad
│   ├── SaleController.java           # Controlador REST
│   ├── SaleItem.java                 # Entidad item
│   ├── SaleItemRepository.java       # Repositorio JPA
│   ├── SaleRepository.java           # Repositorio JPA
│   ├── SaleService.java              # Lógica de negocio
│   └── SaleStatus.java               # Enum de estados
├── security/
│   ├── CustomUserDetailsService.java # UserDetailsService
│   ├── JwtAuthenticationFilter.java  # Filtro JWT
│   └── JwtTokenProvider.java         # Generador/validador JWT
└── user/
    ├── AuthController.java           # Controlador auth
    ├── Role.java                     # Entidad rol
    ├── RoleName.java                 # Enum de roles
    ├── RoleRepository.java           # Repositorio JPA
    ├── User.java                     # Entidad usuario
    ├── UserController.java           # Controlador REST
    ├── UserRepository.java           # Repositorio JPA
    └── UserService.java              # Lógica de negocio
```

## 🔐 Autenticación JWT

El backend utiliza JWT (JSON Web Tokens) para autenticación:

1. El token se genera al hacer login o registro
2. Se retorna con prefijo `Bearer `
3. Para endpoints protegidos, incluir en header:
   ```
   Authorization: Bearer <token>
   ```

**Estructura del Token:**
- **Subject:** Email del usuario
- **Claim "roles":** Roles separados por coma (ej: "ADMIN,VENDEDOR,CLIENTE")
- **Expiración:** 24 horas

## 💰 Cálculo de Ventas

- **Subtotal:** Suma de (precio × cantidad) de cada item
- **IVA:** 19% del subtotal
- **Envío:** $3.990 fijo
- **Total:** Subtotal + IVA + Envío

## 📝 Notas de Desarrollo

- Base de datos H2 en memoria (datos se pierden al reiniciar)
- CORS habilitado para `localhost:3000` y `localhost:5173`
- Lombok para reducir boilerplate
- Validación con Jakarta Bean Validation

## 🛠️ Tecnologías

- **Spring Boot 3.2**
- **Spring Security 6**
- **Spring Data JPA**
- **H2 Database**
- **JWT (jjwt 0.12.3)**
- **Springdoc OpenAPI 2.3**
- **Lombok**
- **Java 17**

---

Desarrollado para **Evaluación Parcial 3 - DSY1104** 🎮
