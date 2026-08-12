# practica2_SA_2026 — OAuth 2.0 + OIDC vs SAML 2.0

Prueba de concepto comparativa de dos estándares de delegación de identidad, implementada con un mismo proveedor de identidad (Keycloak) y dos aplicaciones Spring Boot funcionalmente idénticas.

**Universidad de San Carlos de Guatemala — Centro Universitario de Occidente**
Ingeniería en Ciencias y Sistemas · Software Avanzado

---

## Qué contiene este repositorio

```
practica2_SA_2026/
├── docker-compose.yml      Keycloak (proveedor de identidad)
├── app-oidc/               Aplicación cliente OAuth 2.0 + OpenID Connect
├── app-saml/               Aplicación cliente SAML 2.0
├── EnsayoPractica2.pdf     Ensayo comparativo de ambos protocolos
├── .env.example            Plantilla de variables de entorno
└── .gitignore
```

Ambas aplicaciones tienen exactamente la misma funcionalidad: página pública, página protegida, login y logout. La única diferencia entre ellas es el protocolo de autenticación. Así cualquier diferencia observable es atribuible al protocolo.

---

## Requisitos

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| Docker | 20.10 | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Java (JDK) | 21 | `java -version` |
| OpenSSL | cualquiera | `openssl version` |

Maven no hace falta instalarlo: cada aplicación incluye el wrapper `mvnw`.

**Puertos que deben estar libres:** 3000, 4000 y 8080.

```bash
lsof -i :3000 -i :4000 -i :8080
```

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/EilerGomez/practica2_SA_2026.git
cd practica2_SA_2026
```

### 2. Levantar Keycloak

```bash
docker compose up -d
```

La primera vez descarga la imagen (unos 500 MB). Para seguir el arranque:

```bash
docker compose logs -f keycloak
```

Cuando aparezca `Keycloak 26.0 on JVM started`, sal con `Ctrl+C` (eso no apaga el contenedor).

Verifica en **http://localhost:8080** — usuario `admin`, contraseña `admin`.

### 3. Configurar el realm

Todo esto se hace desde el admin console de Keycloak.

#### 3.1 Crear el realm

Selector superior izquierdo → **Create realm** → nombre: `poc-auth` → **Create**

> Verifica que quedaste dentro de `poc-auth` y no en `master` antes de continuar.

#### 3.2 Crear el usuario de prueba

**Users** → **Create new user**

| Campo | Valor |
|---|---|
| Username | `juan` |
| Email | `juan@poc.local` |
| Email verified | ON |
| First name | `Juan` |
| Last name | `Pérez` |

**Create** → pestaña **Credentials** → **Set password**
- Password: `juan123`
- Temporary: **OFF**

#### 3.3 Crear el client OIDC

**Clients** → **Create client**

- Client type: **OpenID Connect**
- Client ID: `app-oidc`
- **Next**
- Client authentication: **ON**
- Standard flow: **ON**
- **Next**

Login settings:

| Campo | Valor |
|---|---|
| Valid redirect URIs | `http://localhost:3000/login/oauth2/code/keycloak` |
| Valid post logout redirect URIs | `http://localhost:3000/*` |
| Web origins | `http://localhost:3000` |

**Save** → pestaña **Credentials** → copia el **Client secret**.

#### 3.4 Crear el client SAML

**Clients** → **Create client**

- Client type: **SAML**
- Client ID: `app-saml`
- **Next**

Login settings:

| Campo | Valor |
|---|---|
| Root URL | `http://localhost:4000` |
| Valid redirect URIs | `http://localhost:4000/login/saml2/sso/keycloak` |
| Master SAML Processing URL | `http://localhost:4000/login/saml2/sso/keycloak` |

**Save**, luego en la pestaña **Settings** ajusta:

| Opción | Valor |
|---|---|
| Name ID format | `username` |
| Sign documents | ON |
| Sign assertions | ON |

#### 3.5 Mappers del client SAML

En SAML los atributos no viajan automáticamente. Hay que declararlos.

**Clients** → `app-saml` → **Client scopes** → `app-saml-dedicated` → **Configure a new mapper**

**Mapper 1 — Nombre** (tipo *User Property*)

| Campo | Valor |
|---|---|
| Name | `firstName` |
| Property | `firstName` |
| SAML Attribute Name | `firstName` |
| SAML Attribute NameFormat | `Basic` |

**Mapper 2 — Correo** (tipo *User Property*)

| Campo | Valor |
|---|---|
| Name | `email` |
| Property | `email` |
| SAML Attribute Name | `email` |
| SAML Attribute NameFormat | `Basic` |

**Mapper 3 — Roles** (tipo *Role list*)

| Campo | Valor |
|---|---|
| Role attribute name | `Role` |
| SAML Attribute NameFormat | `Basic` |
| Single Role Attribute | OFF |

> Estos tres mappers son el equivalente manual de lo que OIDC entrega automáticamente con `scope=openid profile email`.

### 4. Generar las claves SAML

Desde la raíz del proyecto:

```bash
mkdir -p app-saml/src/main/resources/saml
cd app-saml/src/main/resources/saml

openssl req -newkey rsa:2048 -nodes \
  -keyout sp-private.key \
  -x509 -days 365 \
  -out sp-certificate.crt \
  -subj "/CN=app-saml/O=POC/C=GT"
```

Empaqueta el certificado para Keycloak:

```bash
openssl pkcs12 -export \
  -in sp-certificate.crt \
  -inkey sp-private.key \
  -name app-saml \
  -out sp-keystore.p12 \
  -passout pass:poc123

cd ../../../../..
```

### 5. Registrar el certificado en Keycloak

**Clients** → `app-saml` → pestaña **Keys** → activa **Client signature required**

En el diálogo que aparece:

| Campo | Valor |
|---|---|
| Select method | Import |
| Archive format | **PKCS12** |
| Key alias | `app-saml` |
| Store password | `poc123` |

Sube el archivo `sp-keystore.p12` → **Confirm**

### 6. Variables de entorno

```bash
cp .env.example .env
```

Edita `.env`:

```bash
KEYCLOAK_CLIENT_SECRET=<el secret del paso 3.3>
```

Si vas a usar Google como proveedor adicional, añade también:

```bash
GOOGLE_CLIENT_ID=<tu client id>
GOOGLE_CLIENT_SECRET=<tu client secret>
```

---

## Ejecución

Abre dos terminales.

**Terminal 1 — Aplicación OIDC (puerto 3000):**

```bash
cd app-oidc
export $(cat ../.env | xargs)
./mvnw spring-boot:run
```

**Terminal 2 — Aplicación SAML (puerto 4000):**

```bash
cd app-saml
./mvnw spring-boot:run
```

En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

---

## Verificación

### Prueba 1 — Flujo OIDC

1. Abre **http://localhost:3000**
2. "Página pública" debe abrir sin pedir nada
3. "Página protegida" redirige a Keycloak
4. Entra con `juan` / `juan123`
5. Deberías ver los datos del usuario y el ID token en crudo

**Qué observar:** en la URL de redirección aparecen `response_type=code`, `client_id`, `scope` y `state`.

### Prueba 2 — Flujo SAML

1. Abre **http://localhost:4000**
2. Clic en "Página protegida"

**Qué observar:** si ya iniciaste sesión en la prueba anterior, **Keycloak no te pedirá credenciales**. Eso es Single Sign-On funcionando entre dos protocolos distintos.

### Prueba 3 — Inspección del tráfico

Con las herramientas de desarrollador abiertas en la pestaña **Red**, con *Preserve log* activado:

| Protocolo | Petición de callback | Contenido |
|---|---|---|
| OIDC | `GET /login/oauth2/code/keycloak` | `code` + `state` en la URL |
| SAML | `POST /login/saml2/sso/keycloak` | `SAMLResponse` en el cuerpo del formulario |

Para decodificar la aserción SAML:

```bash
echo "<valor de SAMLResponse>" > saml.b64

python3 -c "
import urllib.parse, base64
import xml.dom.minidom as m
raw = open('saml.b64').read().strip()
xml = base64.b64decode(urllib.parse.unquote(raw)).decode()
print(m.parseString(xml).toprettyxml(indent='  '))
"
```

Hay un ejemplo ya decodificado en `docs/assertion_saml.xml`.

---

## Endpoints de descubrimiento

Ambos protocolos publican su configuración, pero en formatos distintos:

**OIDC** — JSON, ruta estandarizada:
```
http://localhost:8080/realms/poc-auth/.well-known/openid-configuration
```

**SAML** — XML con certificados incrustados:
```
http://localhost:8080/realms/poc-auth/protocol/saml/descriptor
```

---

## Comandos útiles

```bash
docker compose up -d        # Levantar Keycloak
docker compose ps           # Ver estado
docker compose logs -f      # Ver logs en vivo
docker compose stop         # Apagar sin borrar
docker compose down         # Apagar y borrar contenedores
docker compose down -v      # Borrar TODO, incluida la configuración del realm
```

> Cuidado con `down -v`: elimina el volumen y perderías toda la configuración de Keycloak.

Liberar un puerto ocupado:

```bash
kill $(lsof -t -i:4000)
```

---

## Problemas frecuentes

**`BindException: Address already in use`**
Hay otra instancia corriendo en ese puerto. Ver el comando de arriba.

**`relyingPartyRegistrationRepository cannot be null`**
Spring Boot 4 no incluye la autoconfiguración de SAML2 ([issue #18339](https://github.com/spring-projects/spring-security/issues/18339)). Por eso este proyecto define el registro programáticamente en `SamlConfig.java`. Si aparece este error, verifica que esa clase exista y esté anotada con `@Configuration`.

**`Failed to resolve any signing credential`**
El metadata del realm declara `WantAuthnRequestsSigned="true"` y la aplicación no encuentra sus claves. Verifica que `sp-private.key` y `sp-certificate.crt` existan en `app-saml/src/main/resources/saml/`.

**`Invalid redirect uri`**
La URL registrada en Keycloak no coincide con la real. Deben ser idénticas, incluido el puerto.

**El correo llega sin nombre de atributo en la aserción**
Falta llenar *SAML Attribute Name* en el mapper del email. Es un buen ejemplo de que SAML no estandariza nombres de atributos.

**El proyecto aparece como `[unloadable]` en NetBeans**
Ejecuta `./mvnw clean compile` desde terminal para ver el error real, y añade al `pom.xml`:

```xml
<properties>
    <start-class>com.poc.app_saml.AppSamlApplication</start-class>
</properties>
```

---

## Diferencias observables entre ambos POC

| Aspecto | OAuth 2.0 + OIDC | SAML 2.0 |
|---|---|---|
| Dependencia | Casilla en Spring Initializr | Manual + repositorio Shibboleth |
| Autoconfiguración | Incluida en Spring Boot 4 | No incluida — configuración programática |
| Configuración | 6 líneas de properties | Properties + clase Java de ~60 líneas |
| Formato del token | JWT (~800 bytes) | XML (~8,900 bytes) |
| Firmas | Una, del IdP | Dos: Response y Assertion |
| Autenticación del cliente | Client secret compartido | Par de claves, intercambio manual, caduca en 365 días |
| Atributos | Claims estandarizados (`email`, `name`) | Nombres definidos por el implementador |
| Callback | GET con código corto | POST con aserción completa |
| Descubrimiento | JSON en `.well-known/` | XML con certificados incrustados |
| Frontend desacoplado | Soportado (BFF o PKCE) | Solo mediante BFF |

---

## Seguridad

Este proyecto es una prueba de concepto. **No usar en producción tal cual.**

Consideraciones aplicadas:
- Los secretos se leen de variables de entorno, no del código
- Las claves privadas y el archivo `.env` están en `.gitignore`
- Las cookies de sesión usan `HttpOnly`
- La protección CSRF está activa salvo en el endpoint de callback SAML, que recibe un POST originado por el IdP

Pendiente para un entorno real:
- HTTPS obligatorio con HSTS
- Rotación de certificados antes de su caducidad
- Validación estricta del nodo firmado en las aserciones SAML (mitigación de XML Signature Wrapping)
- Almacén de sesiones distribuido si se escala horizontalmente

---

## Referencias

- [RFC 6749 — The OAuth 2.0 Authorization Framework](https://datatracker.ietf.org/doc/html/rfc6749)
- [RFC 7519 — JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [SAML V2.0 Technical Overview — OASIS](https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)
- [OWASP SAML Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SAML_Security_Cheat_Sheet.html)
- [OWASP OAuth 2.0 Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html)
