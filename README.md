# Level-Up Gamer — Backend (microservicios Spring Boot + MySQL)

Backend de la tienda **Level-Up Gamer**: 3 microservicios Spring Boot con MySQL, autenticación con **JWT**, contraseñas cifradas con **BCrypt**, roles **ADMIN / CLIENTE**, empaquetados con **Docker** y pensados para desplegarse en **AWS EC2**. El frontend React consume esta API con **axios** y se publica en **AWS S3**.

## Arquitectura

```
                         ┌──────────────────────────────┐
  Navegador ───────────► │ Frontend React (S3 estático) │
                         └──────────────┬───────────────┘
                                        │ axios (HTTP + JWT en header Authorization)
            ┌───────────────────────────┼───────────────────────────┐
            ▼                           ▼                           ▼
   ┌─────────────────┐       ┌─────────────────┐       ┌──────────────────────┐
   │  ms-productos   │       │   ms-usuarios   │       │ ms-gestion-usuarios  │
   │   :8081         │       │   :8082         │       │   :8083 (solo ADMIN) │
   │ CRUD productos  │       │ registro, login │       │ CRUD de usuarios     │
   └────────┬────────┘       └────────┬────────┘       └──────────┬───────────┘
            │                         └────────────┬──────────────┘
            ▼                                      ▼
   levelup_productos                      levelup_usuarios        (MySQL 8 :3306)
```

Los 3 microservicios comparten la clave `JWT_SECRET`: el token que entrega **ms-usuarios** al iniciar sesión sirve en los otros dos.

| Microservicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `ms-productos` | 8081 | Creación y CRUD de productos | `levelup_productos` |
| `ms-usuarios` | 8082 | Creación de usuarios (registro) e inicio de sesión | `levelup_usuarios` |
| `ms-gestion-usuarios` | 8083 | Gestión de usuarios: listar, editar, cambiar rol, eliminar | `levelup_usuarios` |

Cada microservicio sigue la estructura vista en clases: `model` (entidad JPA) → `repository` (JpaRepository) → `service` (lógica) → `controller` (REST), más `security` (JWT), `config` (seguridad y CORS) y `exception` (errores en JSON).

## Roles y seguridad

- **CLIENTE**: rol de todo usuario que se registra desde la página.
- **ADMIN**: puede crear, editar y eliminar productos, y gestionar usuarios (incluido cambiarles el rol).
- El script de base de datos crea un **ADMIN inicial**:
  - correo: `admin@duocuc.cl`
  - contraseña: `Admin123!` → cámbiala apenas entres (Administración → Editar).
- Un ADMIN no puede eliminarse a sí mismo ni quitarse su propio rol, así siempre queda al menos un administrador.
- Las contraseñas se guardan con **BCrypt** y nunca se devuelven en las respuestas.
- Los correos `@duoc.cl` y `@duocuc.cl` mantienen el **20% de descuento**, pero ya **no** dan permisos de administrador.

## Endpoints

Todas las respuestas de error tienen el formato `{ "mensaje": "..." }`.

### ms-usuarios — `http://<host>:8082`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | Público | Crea un usuario con rol CLIENTE (201). 409 si el correo ya existe |
| POST | `/api/auth/login` | Público | Devuelve `{ token, expiraEn, usuario }`. 401 si las credenciales no son válidas |
| GET | `/api/auth/perfil` | Con token | Datos del usuario logueado |

### ms-productos — `http://<host>:8081`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/productos` | Público | Lista el catálogo |
| GET | `/api/productos/{id}` | Público | Un producto |
| POST | `/api/productos` | ADMIN | Crea un producto (201) |
| PUT | `/api/productos/{id}` | ADMIN | Actualiza un producto |
| DELETE | `/api/productos/{id}` | ADMIN | Elimina un producto (204) |

Body de POST/PUT:
```json
{ "categoria": "Teclados", "nombre": "Teclado mecánico", "descripcion": "Switches rojos", "imagen": "https://ejemplo.com/teclado.jpg", "precio": 49990 }
```

### ms-gestion-usuarios — `http://<host>:8083` (todo requiere ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Lista los usuarios registrados |
| GET | `/api/usuarios/{id}` | Un usuario |
| POST | `/api/usuarios` | Crea un usuario eligiendo el rol (201) |
| PUT | `/api/usuarios/{id}` | Edita datos y rol. Si `contrasena` va vacía, se mantiene la actual |
| DELETE | `/api/usuarios/{id}` | Elimina la cuenta (204) |

Body de POST/PUT:
```json
{ "nombre": "Ana Torres", "correo": "ana@gmail.com", "contrasena": "Clave123!", "telefono": "912345678",
  "region": "Valparaíso", "comuna": "Viña del Mar", "rol": "CLIENTE" }
```

Respuestas de seguridad: **401** sin token o con token inválido/expirado, **403** con token válido pero sin el rol necesario.

## Estructura del repositorio

```
levelup-backend/
├── docker-compose.yml          # MySQL + 3 microservicios
├── .env.example                # Variables (copiar como .env)
├── db/init.sql                 # Script de BD: tablas, ADMIN inicial y catálogo
├── postman/LevelUpGamer.postman_collection.json
├── ms-productos/        (Dockerfile, pom.xml, src/)
├── ms-usuarios/         (Dockerfile, pom.xml, src/)
└── ms-gestion-usuarios/ (Dockerfile, pom.xml, src/)
```

## 1. Ejecutar en tu PC (Docker Desktop)

```bash
cd levelup-backend
docker compose up -d --build
docker compose ps            # los 4 contenedores deben quedar "Up" (mysql "healthy")
docker compose logs -f ms-usuarios
```

Prueba en el navegador: <http://localhost:8081/api/productos>

La primera vez MySQL ejecuta `db/init.sql` automáticamente. Si cambias el script y quieres que se vuelva a ejecutar, borra el volumen con `docker compose down -v` (**esto borra los datos**).

### Sin Docker (IntelliJ / VS Code)

1. Ten MySQL corriendo en `localhost:3306` (usuario `root`, clave `root`, o cambia `DB_USER` y `DB_PASSWORD`).
2. Ejecuta `db/init.sql` en MySQL Workbench.
3. Corre cada microservicio con `mvn spring-boot:run` dentro de su carpeta (o el botón Run de tu IDE).

## 2. Pruebas unitarias y cobertura

Las pruebas usan una base **H2 en memoria**, así que no necesitan MySQL. En cada microservicio:

```bash
cd ms-productos
mvn test
```

El informe de cobertura (JaCoCo) queda en `target/site/jacoco/index.html`. Ábrelo en el navegador y toma la captura para la evidencia. Hay pruebas de servicio (Mockito) y de controlador (MockMvc con seguridad real: 401, 403, 400, 404 y 409).

## 3. Desplegar en AWS EC2 con Docker (basado en la guía del curso)

### 3.1 Crear la instancia

| Campo | Valor |
|---|---|
| AMI | Amazon Linux 2023 |
| Tipo | **t3.small** o **t3.medium** si tu laboratorio lo permite. En **t2.micro** (1 GB) MySQL + 3 JVM no caben: crea el swap del paso 3.3 |
| Key pair | Vockey |
| Almacenamiento | 16 GB o más |

**Security Group (reglas de entrada):**

| Tipo | Puerto | Origen |
|---|---|---|
| SSH | 22 | My IP |
| Custom TCP | 8081 | Anywhere (0.0.0.0/0) |
| Custom TCP | 8082 | Anywhere (0.0.0.0/0) |
| Custom TCP | 8083 | Anywhere (0.0.0.0/0) |

No abras el 3306 a Internet: los microservicios llegan a MySQL por la red interna de Docker.

Asigna una **Elastic IP** (EC2 → Elastic IPs → Allocate → Associate) y anótala como `<ELASTIC_IP>`.

### 3.2 Conectarse e instalar Docker

```bash
chmod 400 Vockey.pem
ssh -i "Vockey.pem" ec2-user@<ELASTIC_IP>

sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

exit   # sal y vuelve a entrar para que el grupo docker se aplique
ssh -i "Vockey.pem" ec2-user@<ELASTIC_IP>
docker compose version
```

### 3.3 (Recomendado en instancias chicas) Memoria swap de 2 GB

```bash
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h
```

### 3.4 Clonar y levantar

```bash
git clone https://github.com/<tu-usuario>/levelup-backend.git
cd levelup-backend
cp .env.example .env
nano .env        # cambia MYSQL_ROOT_PASSWORD, JWT_SECRET y CORS_ORIGINS (URL de tu bucket S3)
docker compose up -d --build
docker compose ps
docker compose logs -f
```

La primera compilación tarda varios minutos. Cuando veas `Started Ms...Application`, prueba desde tu PC:

- `http://<ELASTIC_IP>:8081/api/productos`
- Postman: cambia las variables `url_productos`, `url_usuarios` y `url_gestion` a `http://<ELASTIC_IP>:8081`, `:8082` y `:8083`.

### 3.5 Actualizar después de un cambio

```bash
cd ~/levelup-backend
git pull
docker compose up -d --build
```

## 4. Publicar el frontend en AWS S3

1. En el proyecto React (`app-duoc`), crea `.env.production` con la IP de la EC2:
   ```
   VITE_API_PRODUCTOS_URL=http://<ELASTIC_IP>:8081
   VITE_API_USUARIOS_URL=http://<ELASTIC_IP>:8082
   VITE_API_GESTION_URL=http://<ELASTIC_IP>:8083
   ```
2. Compila: `npm install` y luego `npm run build`. Se genera la carpeta `dist/`.
3. AWS → S3 → **Create bucket** (ej: `levelup-gamer-front`, región `us-east-1`). Desmarca **Block all public access** y confirma.
4. Bucket → **Properties** → **Static website hosting** → Enable. Index document: `index.html`. Error document: `index.html` (necesario para que funcionen rutas como `/Login` o `/Admin/Usuarios` al recargar).
5. Bucket → **Permissions** → **Bucket policy** (cambia el nombre del bucket):
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Sid": "PublicRead",
       "Effect": "Allow",
       "Principal": "*",
       "Action": "s3:GetObject",
       "Resource": "arn:aws:s3:::levelup-gamer-front/*"
     }]
   }
   ```
6. **Upload** → sube el **contenido** de `dist/` (el `index.html` y la carpeta `assets/`, no la carpeta `dist` en sí). Con AWS CLI: `aws s3 sync dist/ s3://levelup-gamer-front --delete`.
7. La URL pública aparece en Properties → Static website hosting, por ejemplo `http://levelup-gamer-front.s3-website-us-east-1.amazonaws.com`.
8. En la EC2, pon esa URL en `CORS_ORIGINS` dentro de `.env` y aplica el cambio con `docker compose up -d`.

> El sitio de S3 se sirve por **HTTP**, igual que la API en la EC2, así que el navegador no bloquea las llamadas. Si más adelante pones el frontend detrás de CloudFront (HTTPS), la API también tendría que tener HTTPS.

## Problemas frecuentes

| Síntoma | Qué revisar |
|---|---|
| El frontend muestra "No se pudo conectar con el servidor" | ¿Están `Up` los contenedores (`docker compose ps`)? ¿Abriste 8081-8083 en el Security Group? ¿La IP del `.env.production` es la Elastic IP? |
| Error de CORS en la consola del navegador | `CORS_ORIGINS` debe tener la URL exacta del bucket (sin `/` al final). Luego `docker compose up -d` |
| 401 en todo después de un tiempo | El token expira a las 8 horas: vuelve a iniciar sesión |
| Un microservicio se reinicia en bucle | `docker compose logs ms-usuarios`. Casi siempre MySQL no estaba listo o falta memoria (usa swap) |
| El build se cuelga en t2.micro | Crea el swap (3.3) o compila las imágenes en tu PC y súbelas a Docker Hub (ver guía del curso) |
| Login del admin no funciona | El script solo corre con el volumen vacío: `docker compose down -v` y `docker compose up -d` (borra los datos) |
| Disco lleno | `docker system prune -af` |

## Checklist de la pauta

- [x] Login con roles ADMIN / CLIENTE, contraseñas con hash (BCrypt), rutas y endpoints protegidos (JWT).
- [x] CRUD de productos y CRUD de usuarios (este último solo ADMIN).
- [x] Front en React conectado a la API Spring Boot + MySQL con axios (GET, POST, PUT, DELETE), sin datos quemados.
- [x] Pruebas unitarias con informe de cobertura: JaCoCo (backend) y Vitest (frontend, `npm run test:coverage`).
- [ ] Despliegue en la nube: EC2 (backend) y S3 (frontend) → sigue las secciones 3 y 4.
- [x] README, colección Postman (`postman/`), script de BD (`db/init.sql`).
- [ ] ERS y evidencias de la demo (capturas de Postman, cobertura y la página funcionando).
