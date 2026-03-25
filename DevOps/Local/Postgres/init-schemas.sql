-- SmartCare PostgreSQL Schema Initialization
-- Auto-runs on first container start via docker-entrypoint-initdb.d

-- Admin database schemas
CREATE SCHEMA IF NOT EXISTS smartcare_admin_db;

-- Messaging broker schema
CREATE SCHEMA IF NOT EXISTS messaging_broker;

-- Application databases
CREATE DATABASE smartcare_broker;
\c smartcare_broker;
CREATE SCHEMA IF NOT EXISTS messaging_broker;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA smartcare_admin_db TO smartcare;
GRANT ALL PRIVILEGES ON SCHEMA messaging_broker TO smartcare;
