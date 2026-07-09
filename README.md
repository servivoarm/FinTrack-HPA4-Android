# FinTrack - Aplicación de Gestión Financiera Personal

FinTrack es una aplicación móvil nativa para Android orientada a la gestión de finanzas personales. Permite registrar ingresos, gastos, presupuestos por categoría, objetivos de ahorro, reportes mensuales e historial filtrable.

## Descripción

La aplicación fue desarrollada como proyecto final de la asignatura Herramientas de Programación Aplicada IV. Su objetivo es ayudar al usuario a organizar sus movimientos financieros de forma sencilla, visual y local, sin depender de servicios bancarios externos.

## Funcionalidades principales

- Registro e inicio de sesión local.
- Perfil de usuario con foto, correo, nombre y moneda preferida.
- Registro de ingresos únicos, semanales, quincenales y mensuales.
- Registro de gastos únicos, semanales, quincenales y mensuales.
- Cálculo automático de saldo disponible.
- Presupuestos mensuales por categoría.
- Alertas al alcanzar el 80% o superar el 100% del presupuesto.
- Objetivos de ahorro con aportes.
- Gráficas de línea para evolución financiera.
- Gráficas de dona para distribución de ingresos, gastos y ahorros.
- Reporte mensual con ingresos, gastos, ahorros y balance.
- Historial filtrable por tipo, categoría y rango de fechas.
- Notificaciones para recordar gastos futuros.
- Cambio de moneda con conversión visual.
- Persistencia local con Room y SharedPreferences.

## Tecnologías utilizadas

- Kotlin
- Android Studio
- XML Layouts
- ViewBinding
- Room Database
- SharedPreferences
- MVVM
- Coroutines
- ViewModel
- RecyclerView
- AlarmManager
- NotificationManager
- Canvas personalizado para gráficas

## Arquitectura general

El proyecto utiliza una arquitectura basada en MVVM:

- **View:** Activities, Fragments y layouts XML.
- **ViewModel:** Manejo de estado y lógica de presentación.
- **Repository:** Comunicación entre ViewModel y fuentes de datos.
- **Room Database:** Persistencia local de usuarios, movimientos, presupuestos y objetivos de ahorro.
- **SharedPreferences:** Sesión, moneda y preferencias del usuario.

## Módulos principales

### Autenticación

Permite registrar usuarios, iniciar sesión, cerrar sesión y mantener preferencias locales.

### Home

Muestra el resumen financiero general, saldo disponible, ingresos, gastos, ahorros, gráficas y accesos rápidos.

### Ingresos

Permite registrar, editar, eliminar y visualizar ingresos. Incluye gráficas por tiempo y distribución por categoría.

### Gastos

Permite registrar, editar, eliminar y visualizar gastos. Incluye recordatorios de pago y análisis por categoría.

### Presupuestos

Permite crear presupuestos mensuales por categoría, calcular porcentaje usado y mostrar alertas visuales.

### Ahorros

Permite crear objetivos de ahorro, registrar aportes y visualizar el avance acumulado.

### Reportes

Muestra un resumen mensual con ingresos, gastos, ahorros, balance final, gráficas y opción de compartir.

### Historial

Permite consultar todos los movimientos financieros y filtrarlos por tipo, categoría y fechas.

## Integrantes

- Arlin Serrano
- Oscar Montes
- José Luis Silvera

## Asignatura

Herramientas de Programación Aplicada IV  
Universidad Tecnológica de Panamá  
Grupo 1IL143  
I Semestre 2026

## Estado del proyecto

Proyecto funcional para entrega académica.
