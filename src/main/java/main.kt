import kotlin.system.exitProcess

fun main() {
    println("Bienvenido al generador de gráficos y datos del COVID-19 en España")
    println("Preparando datos")
    val datatable = DataTable()
    datatable.createTable()
    println("Datos preparados")
    while (true) {
        when (askForGraphics()) {
            "1" -> datatable.getWhatToCalculate(false)

            "2" -> {
                when(whatGraphics()){
                    "1" -> datatable.getWhatToCalculate(true)
                    "2" -> {datatable.getWhereToShowGraphics()}
                }
            }
        }
    }
}

fun whatGraphics(): String{
    println("¿Qué datos te gustaría ver?\n")
    var answer = "C"
    while(answer.toUpperCase() != "S") {
        println("1) Totales, máximos o medias nacionales y/o de comunidades\t")
        println("2) Progresión en días de comunidades y/o nacionales")
        println("s) Volver al menú anterior")
        answer = readLine()!!
        when (answer){
            "1", "2", "s", "S" -> return answer
            else -> println("El valor no se ha reconocido\n\n")
        }
    }
    return answer
}

fun askForGraphics(): String {
    var answer = "C"
    while (answer.toUpperCase() != "S") {
        println("\n\n\n¿Que te gustaría hacer")
        println("1) Obtener datos numéricos")
        println("2) Obtener gráficas a partir de datos")
        println("s) Salir del programa")
        answer = readLine()!!
        when (answer) {
            "1", "2" -> return answer
            "s" -> exitProcess(0)
            else -> println("El valor introducido no es correcto\n")
        }
    }
    return answer
}
