// =============================================================================
// Demo CRUD (MongoDB) — colección "maquina"
// Demuestra las operaciones que pide la consigna del Proyecto Integrador:
// insertar documentos con estructuras variadas, búsquedas filtradas,
// actualización de registros y eliminación de datos específicos.
//
// El script es autocontenido y re-ejecutable: trabaja sobre documentos de
// demostración (_id 99901-99903) que crea al inicio y elimina al final,
// sin tocar los datos reales de la aplicación.
//
// Ejecutar desde la shell del contenedor (consigna: acceso por línea de comandos):
//   docker exec -i egi-mongodb mongosh inventario_egi --quiet < migraciones/mongodb/demo_crud.js
// =============================================================================

const SPRING_CLASS = "com.itu.egi.inventarioseguro.model.MaquinaHardware";

// --- 1) INSERT: documentos con estructuras variadas -------------------------
print("\n=== 1) insertMany: documentos con estructuras variadas ===");
db.maquina.deleteMany({ _id: { $in: [NumberLong("99901"), NumberLong("99902"), NumberLong("99903")] } });
db.maquina.insertMany([
  {
    _id: NumberLong("99901"),
    fabricante: "Dell",
    modelo: "Latitude 5440",
    tipo: "LAPTOP",
    cpu: "Intel Core i7-1355U",
    ramGb: 32,
    disco: { tipo: "SSD", capacidadGb: 1000 },
    sistemaOperativo: "Windows 11 Pro",
    // sin perifericos: estructura distinta a la de un desktop
    _class: SPRING_CLASS,
  },
  {
    _id: NumberLong("99902"),
    fabricante: "HP",
    modelo: "EliteDesk 800 G9",
    tipo: "DESKTOP",
    cpu: "Intel Core i9-12900",
    ramGb: 64,
    disco: { tipo: "SSD", capacidadGb: 2000 },
    sistemaOperativo: "Debian 12",
    perifericos: { monitor: 'HP E27 27"', mouse: "HP 128", teclado: "HP 125" },
    _class: SPRING_CLASS,
  },
  {
    _id: NumberLong("99903"),
    fabricante: "Apple",
    modelo: "iMac 24",
    tipo: "ALL_IN_ONE",
    cpu: "Apple M3",
    ramGb: 16,
    disco: { tipo: "SSD", capacidadGb: 512 },
    sistemaOperativo: "macOS Sonoma",
    perifericos: { mouse: "Magic Mouse", teclado: "Magic Keyboard" },
    _class: SPRING_CLASS,
  },
]);
print(`Documentos demo insertados. Total en colección: ${db.maquina.countDocuments()}`);

// --- 2) FIND: búsquedas filtradas --------------------------------------------
print("\n=== 2) Búsquedas filtradas ===");

print("\n-- Laptops:");
db.maquina.find({ tipo: "LAPTOP" }).forEach((d) => printjson(d));

print("\n-- Máquinas con 32 GB de RAM o más (proyección: fabricante, modelo, ramGb):");
db.maquina
  .find({ ramGb: { $gte: 32 } }, { fabricante: 1, modelo: 1, ramGb: 1 })
  .forEach((d) => printjson(d));

print("\n-- Máquinas con disco SSD de 1 TB o más (filtro sobre objeto embebido):");
db.maquina
  .find({ "disco.tipo": "SSD", "disco.capacidadGb": { $gte: 1000 } }, { modelo: 1, disco: 1 })
  .forEach((d) => printjson(d));

print("\n-- Máquinas SIN periféricos cargados ($exists sobre campo opcional):");
db.maquina
  .find({ perifericos: { $exists: false } }, { fabricante: 1, modelo: 1, tipo: 1 })
  .forEach((d) => printjson(d));

print("\n-- Modelos que empiezan con 'Elite' (expresión regular):");
db.maquina.find({ modelo: { $regex: /^Elite/ } }, { modelo: 1 }).forEach((d) => printjson(d));

// --- 3) UPDATE: actualización de registros -----------------------------------
print("\n=== 3) Updates ===");

print("\n-- updateOne: ampliación de RAM de la 99901 (32 -> 64 GB):");
db.maquina.updateOne({ _id: NumberLong("99901") }, { $set: { ramGb: 64 } });
printjson(db.maquina.findOne({ _id: NumberLong("99901") }, { modelo: 1, ramGb: 1 }));

print("\n-- updateMany: se reinstala Windows 11 Pro en todos los desktops demo:");
const up = db.maquina.updateMany(
  { _id: { $gte: NumberLong("99901") }, tipo: "DESKTOP" },
  { $set: { sistemaOperativo: "Windows 11 Pro" } }
);
print(`Documentos modificados: ${up.modifiedCount}`);

// --- 4) DELETE: eliminación de datos específicos ------------------------------
print("\n=== 4) Deletes ===");

print("\n-- deleteOne: se da de baja la iMac (_id 99903):");
const del = db.maquina.deleteOne({ _id: NumberLong("99903") });
print(`Documentos eliminados: ${del.deletedCount}`);

// Limpieza final: se eliminan los documentos demo restantes.
const cleanup = db.maquina.deleteMany({ _id: { $gte: NumberLong("99901") } });
print(`\nLimpieza: ${cleanup.deletedCount} documentos demo restantes eliminados.`);
print(`Total final en colección: ${db.maquina.countDocuments()}`);
