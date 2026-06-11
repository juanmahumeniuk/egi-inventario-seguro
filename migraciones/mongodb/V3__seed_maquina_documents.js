// =============================================================================
// Migración V3 (MongoDB): documentos de hardware para las máquinas seed de SQL.
// Inserta documentos con estructuras variadas (consigna del proyecto):
//   - _id 2: desktop con HDD y periféricos completos
//   - _id 3: laptop SIN periféricos (campo opcional ausente)
//   - _id 4: all-in-one con periféricos parciales (monitor integrado)
//
// El _id de cada documento es el mismo id de la tabla Maquina en SQL Server.
// El script es idempotente: borra y vuelve a insertar estos tres documentos.
//
// Ejecutar con: mongosh "<connection-string>" migraciones/mongodb/V3__seed_maquina_documents.js
// =============================================================================

const SPRING_CLASS = "com.itu.egi.inventarioseguro.model.MaquinaHardware";

const documentos = [
  {
    _id: NumberLong("2"),
    fabricante: "HP",
    modelo: "ProDesk 400 G7",
    tipo: "DESKTOP",
    cpu: "Intel Core i5-10500",
    ramGb: 8,
    disco: { tipo: "HDD", capacidadGb: 1000 },
    sistemaOperativo: "Windows 10 Pro",
    perifericos: {
      monitor: 'HP V24i 24"',
      mouse: "HP 128",
      teclado: "HP 125",
    },
    _class: SPRING_CLASS,
  },
  {
    _id: NumberLong("3"),
    fabricante: "Lenovo",
    modelo: "ThinkPad E14 Gen 5",
    tipo: "LAPTOP",
    cpu: "AMD Ryzen 5 7530U",
    ramGb: 16,
    disco: { tipo: "SSD", capacidadGb: 512 },
    sistemaOperativo: "Ubuntu 22.04 LTS",
    // Sin periféricos: la laptop usa pantalla, teclado y touchpad integrados.
    _class: SPRING_CLASS,
  },
  {
    _id: NumberLong("4"),
    fabricante: "Lenovo",
    modelo: "IdeaCentre AIO 3 24",
    tipo: "ALL_IN_ONE",
    cpu: "AMD Ryzen 7 5700U",
    ramGb: 16,
    disco: { tipo: "SSD", capacidadGb: 256 },
    sistemaOperativo: "Windows 11 Home",
    perifericos: {
      // Monitor integrado en el equipo: solo mouse y teclado externos.
      mouse: "Lenovo Calliope",
      teclado: "Lenovo Calliope",
    },
    _class: SPRING_CLASS,
  },
];

const ids = documentos.map((d) => d._id);
db.maquina.deleteMany({ _id: { $in: ids } });
const resultado = db.maquina.insertMany(documentos);
print(`V3: ${resultado.insertedIds ? Object.keys(resultado.insertedIds).length : 0} documentos de hardware insertados (ids: ${ids.join(", ")}).`);
