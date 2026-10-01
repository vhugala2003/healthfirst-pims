# HealthFirst PIMS

A polished Java Swing desktop app for pharmacy stock control, checkout, and operational reporting. Built with JDBC and MySQL 8 for the PRO732 Pharmacy Inventory Management System assignment.

## Features

- Role-aware sign-in for administrators and cashiers, with PBKDF2 password verification.
- Admin workspace for medicine, supplier, and cashier-account management.
- Cashier point of sale with stock lookup, cart totals, transactional checkout, and printable or saveable receipts.
- Sales, item-wise, low-stock, and next-30-day expiry reports.
- Sample catalog data, relational constraints, stock safeguards, and a MySQL Docker Compose setup.

## Demo Accounts

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `admin123` |
| Cashier | `cashier` | `cash123` |

Change these demo passwords before using the system beyond a local demonstration.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- MySQL 8, or Docker Desktop with Docker Compose

## Run Locally

Start MySQL with the included sample database:

```powershell
docker compose up -d
```

Set the JDBC connection environment variables in PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER = "root"
$env:DB_PASSWORD = "local_dev_only"
```

Build and launch:

```powershell
mvn clean package
java -jar target/healthfirst-pims.jar
```

For an existing MySQL installation, import `database.sql` and set `DB_URL`, `DB_USER`, and `DB_PASSWORD` to match that database. The application never creates a default database connection or embeds production credentials.

## Windows Executable

After building on Windows with a JDK that includes `jpackage`, run:

```powershell
.\scripts\build-windows.ps1
```

The script creates an installer under `dist/`. MySQL remains a separately installed service or Docker container; the installer does not bundle a database server.
GitHub Actions also builds the Windows installer and publishes it as a downloadable workflow artifact after pushes to `main`.

## Database Model

The schema uses `users`, `suppliers`, `medicines`, `sales`, and `sale_items`. Sale line items preserve the price at checkout, while medicine stock is decremented inside the same database transaction as the sale. Referenced sales are retained when a medicine is no longer sold, and cashier accounts with history should be disabled rather than erased.

## Notes

This is an educational project. Sample suppliers, medicines, and contact details are fictional. Do not use the demo credentials or this application as a production pharmacy or clinical system without security review, backups, privacy assessment, and operational validation.