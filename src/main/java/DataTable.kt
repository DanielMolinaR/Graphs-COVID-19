import tech.tablesaw.aggregate.AggregateFunctions
import tech.tablesaw.api.*
import tech.tablesaw.api.ColumnType.*
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.io.csv.CsvReadOptions
import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.api.HorizontalBarPlot
import tech.tablesaw.plotly.api.TimeSeriesPlot
import java.net.URL
import java.time.LocalDate


class DataTable {
    private var table: Table
    private var initialTable: Table
    private val communities = Communities()

    init {
        this.table = getCSV()
        this.initialTable = getCSV()
    }

    fun createTable() {
        this.table = editTable(this.table)
    }

    private fun getCSV(): Table {
        val location = "https://daniel.halogenos.org/agregados.csv"
        val types = arrayOf<ColumnType>(STRING, LOCAL_DATE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE)
        return Table.read().usingOptions(CsvReadOptions.builder(URL(location)).separator(',')
                .header(true).missingValueIndicator(null).columnTypes(types))

    }

    private fun editTable(t: Table): Table {
        var finalTable = Table.create("COVID-19", StringColumn.create("CCAA"),
                DateColumn.create("FECHA"), DoubleColumn.create("Casos"), DoubleColumn.create("Casos acumulado"),
                DoubleColumn.create("Altas"), DoubleColumn.create("Altas acumulado"), DoubleColumn.create("PCR+"),
                DoubleColumn.create("PCR+ acumulado"), DoubleColumn.create("TestAc+"),
                DoubleColumn.create("TestAc+ acumulado"), DoubleColumn.create("Hospitalizados"),
                DoubleColumn.create("Hospitalizados acumulado"), DoubleColumn.create("UCI"),
                DoubleColumn.create("UCI acumulado"), DoubleColumn.create("Fallecidos"),
                DoubleColumn.create("Fallecidos acumulado"))

        if (!t.isEmpty) {
            for (row in t)
                finalTable = addingData(row, finalTable)

            finalTable = finalTable.sortOn(0, 1)
            finalTable = calculateDailyData(finalTable)
        }
        return finalTable
    }

    private fun addingData(r: Row, t: Table?): Table? {
        if (t != null) {
            t.column("CCAA").appendObj(r.getString("CCAA"))
            t.column("FECHA").appendObj(r.getDate("FECHA"))
            t.column("Casos acumulado").appendObj(r.getDouble("Casos"))
            t.column("Altas acumulado").appendObj(r.getDouble("Altas"))
            t.column("PCR+ acumulado").appendObj(r.getDouble("PCR+"))
            t.column("TestAc+ acumulado").appendObj(r.getDouble("TestAc+"))
            t.column("Hospitalizados acumulado").appendObj(r.getDouble("Hospitalizados"))
            t.column("UCI acumulado").appendObj(r.getDouble("UCI"))
            t.column("Fallecidos acumulado").appendObj(r.getDouble("Fallecidos"))
            t.column("Casos").appendObj(null)
            t.column("Altas").appendObj(null)
            t.column("PCR+").appendObj(null)
            t.column("TestAc+").appendObj(null)
            t.column("Hospitalizados").appendObj(null)
            t.column("UCI").appendObj(null)
            t.column("Fallecidos").appendObj(null)
        }
        return t
    }

    private fun calculateDailyData(t: Table?): Table? {
        if (t != null) {
            var value: Double
            var tmpCCAA = ""
            for (c in 1 until 16) {
                if (c % 2 == 0) {
                    for (i in 0 until t.rowCount()) {
                        if (tmpCCAA != t.row(i).getString("CCAA")) {
                            tmpCCAA = t.row(i).getString("CCAA")
                            value = 0.0
                        } else {
                            value = t.row(i).getDouble(c + 1) - t.row(i - 1).getDouble(c + 1)
                        }
                        t.row(i).setDouble(c, value)
                    }
                }
            }
        }
        return t
    }


    private fun generateCountryTable(initialDate: LocalDate, finalDate: LocalDate, whatToCalculate: Int): Table? {
        val finalTable = this.table.where(this.table.dateColumn("FECHA").isBetweenIncluding(initialDate, finalDate))
        return when (whatToCalculate) {
            3 -> {
                finalTable.setName("Total")
                summarizingData(finalTable)
            }
            2 -> {
                finalTable.setName("Max")
                generateMaxCountry(finalTable)
            }
            else -> {
                finalTable.setName("Average")
                averageData(finalTable)
            }
        }
    }

    private fun generateCommunityTable(communityList: List<String>, initialDate: LocalDate, finalDate: LocalDate, whatToCalculate: Int): Table? {
        var finalTable = when (whatToCalculate) {
            3 -> {
                Table.create("Totales entre comunidades", DoubleColumn.create("Sum [Casos]"),
                        DoubleColumn.create("Sum [PCR+]"), DoubleColumn.create("Sum [TestAc+]"),
                        DoubleColumn.create("Sum [Altas]"), DoubleColumn.create("Sum [Hospitalizados]"),
                        DoubleColumn.create("Sum [Fallecidos]"), DoubleColumn.create("Sum [UCI]"))
            }
            2 -> {
                Table.create("Máximos entre comunidades", StringColumn.create("CCAA"),
                        DateColumn.create("FECHA"), StringColumn.create("Campo"), DoubleColumn.create("Máximo"))
            }
            else -> {
                Table.create("Media entre comunidades", DoubleColumn.create("Mean [Casos]"),
                        DoubleColumn.create("Mean [PCR+]"), DoubleColumn.create("Mean [TestAc+]"),
                        DoubleColumn.create("Mean [Altas]"), DoubleColumn.create("Mean [Hospitalizados]"),
                        DoubleColumn.create("Mean [Fallecidos]"), DoubleColumn.create("Mean [UCI]"))
            }
        }
        for (c in communityList) {
            var auxTable = this.table.where(this.table.stringColumn("CCAA").isEqualTo(c))
            auxTable = auxTable.where(auxTable.dateColumn("FECHA").isBetweenIncluding(initialDate, finalDate))

            when (whatToCalculate) {
                3 -> finalTable.append(summarizingData(auxTable))

                2 -> {
                    if (communityList.size > 1)
                        finalTable = compareAndGetBigger(finalTable, generateMaxCommunity(auxTable))
                    else
                        finalTable.append(generateMaxCommunity(auxTable))

                }

                else -> {
                    finalTable.append(averageData(auxTable))
                }
            }
        }
        if (whatToCalculate != 2)
            finalTable.addColumns(StringColumn.create("CCAA", communityList))

        return finalTable

    }

    @Suppress("DuplicatedCode")
    private fun compareAndGetBigger(t1: Table?, t2: Table?): Table? {

        if (t1 != null) {
            if (t1.column(0).size() > 0)
                if (t2 != null) {
                    return if (t2.column(0).size() > 0) {
                        val tempTable = Table.create("Máximos entre comunidades", StringColumn.create("CCAA"),
                                DateColumn.create("FECHA"), StringColumn.create("Campo"), DoubleColumn.create("Máximo"))
                        for (index in 0 until 7) {
                            if (t1.row(index).getDouble("Máximo") > t2.row(index).getDouble("Máximo")) {
                                tempTable.column("CCAA").appendObj(t1.row(index).getString("CCAA"))
                                tempTable.column("FECHA").appendObj(t1.row(index).getDate("FECHA"))
                                tempTable.column("Campo").appendObj(t1.row(index).getString("Campo"))
                                tempTable.column("Máximo").appendObj(t1.row(index).getDouble("Máximo"))
                            } else {
                                tempTable.column("CCAA").appendObj(t2.row(index).getString("CCAA"))
                                tempTable.column("FECHA").appendObj(t2.row(index).getDate("FECHA"))
                                tempTable.column("Campo").appendObj(t2.row(index).getString("Campo"))
                                tempTable.column("Máximo").appendObj(t2.row(index).getDouble("Máximo"))
                            }
                        }
                        tempTable
                    } else
                        t1
                } else return null
        }
        if (t2 != null)
            if (t2.column(0).size() > 0)
                return t2
        return null
    }

    private fun summarizingData(t: Table): Table? {
        return t.summarize("Casos", "Altas", "PCR+",
                "TestAc+", AggregateFunctions.sum).apply().concat(
                t.summarize("Hospitalizados", "UCI",
                        "Fallecidos", AggregateFunctions.sum).apply())
    }

    private fun averageData(t: Table): Table? {
        return t.summarize("Casos", "Altas", "PCR+",
                "TestAc+", AggregateFunctions.mean).apply().concat(
                t.summarize("Hospitalizados", "UCI",
                        "Fallecidos", AggregateFunctions.mean).apply())
    }

    private fun generateMaxCountry(t: Table): Table? {
        val finalTable = Table.create("Máximos Nacionales", DateColumn.create("FECHA"),
                StringColumn.create("Campo"), DoubleColumn.create("Máximo"))
        var auxTable = t.copy()
        for (c in 2 until 16) {
            if (c % 2 == 0) {
                val columnName = t.column(c).name()
                auxTable = auxTable.sortDescendingOn(columnName)
                finalTable.column("FECHA").appendObj(auxTable.row(0).getDate("FECHA"))
                finalTable.column("Campo").appendObj(columnName)
                finalTable.column("Máximo").appendObj(auxTable.row(0).getDouble(columnName))
            }
        }
        return finalTable

    }

    private fun generateMaxCommunity(t: Table): Table? {
        val finalTable = Table.create("Máximos entre comunidades", StringColumn.create("CCAA"),
                DateColumn.create("FECHA"), StringColumn.create("Campo"), DoubleColumn.create("Máximo"))
        var auxTable = t.copy()
        for (c in 2 until 16) {
            if (c % 2 == 0) {
                val columnName = t.column(c).name()
                auxTable = auxTable.sortDescendingOn(columnName)
                finalTable.column("CCAA").appendObj(auxTable.row(0).getString("CCAA"))
                finalTable.column("FECHA").appendObj(auxTable.row(0).getDate("FECHA"))
                finalTable.column("Campo").appendObj(columnName)
                finalTable.column("Máximo").appendObj(auxTable.row(0).getDouble(columnName))
            }
        }
        return finalTable
    }

    private fun askWhatToShow(): Int {
        println("¿Qué datos te gustaría ver?")
        println("1) Media \t 2) Máximo \t 3) Totales\n")
        var answer = "C"
        while (answer.toUpperCase() != "S") {
            answer = readLine()!!
            when (answer) {
                "1", "2", "3" -> {
                    return answer.toInt()
                }
                else -> {
                    println("El valor introducido no es correcto, vuelve a intentarlo\n")
                }
            }
        }
        return -1
    }

    private fun checkDateIsCorrect(dateToCheck: LocalDate, firstDate: LocalDate?): Boolean {
        val minDate = this.table.row(0).getDate("FECHA")
        val maxDate = this.table.row(this.table.rowCount() - 1).getDate("FECHA")
        if (firstDate != null) {
            if (dateToCheck < firstDate)
                return false
        }
        if (dateToCheck < minDate)
            return false
        if (dateToCheck > maxDate)
            return false
        return true
    }

    private fun selectDate(needsTwoDates: Boolean, isSecondDate: Boolean, firstDate: LocalDate?): LocalDate {
        var finish = false
        var date: LocalDate = LocalDate.now()
        while (!finish) {
            if (needsTwoDates) {
                if (isSecondDate) {
                    try {
                        println("Introduce una fecha final para el intervalo con formato mes-dia MM-DD (Ejemplo: 05-25)")
                        date = LocalDate.parse("2020-" + readLine()!!)
                        if (checkDateIsCorrect(date, firstDate))
                            finish = true
                    } catch (e: Exception) {
                        println("El valor introducido no se reconoce como fecha")
                    }
                } else {
                    try {
                        println("Introduce una fecha inicial para el intervalo con formato mes-dia MM-DD (Ejemplo: 05-25)")
                        date = LocalDate.parse("2020-" + readLine()!!)
                        if (checkDateIsCorrect(date, null))
                            finish = true
                    } catch (e: Exception) {
                        println("El valor introducido no se reconoce como fecha")
                    }
                }
            } else {
                try {
                    println("Introduce una fecha con formato mes-dia MM-DD (Ejemplo: 05-25)")
                    date = LocalDate.parse("2020-" + readLine()!!)
                    if (checkDateIsCorrect(date, null))
                        finish = true
                } catch (e: Exception) {
                    println("El valor introducido no se reconoce como fecha")
                }
            }
        }
        return date
    }

    private fun askDate(): String {
        println("¿Quieres seleccionar un periodo de tiempo o una fecha específica?")
        println("1 = Periodo de tiempo")
        println("2 = Fecha especifica")
        println("3 = Todos los registros")
        return readLine()!!
    }


    fun getWhatToCalculate(isGraphics: Boolean) {
        val community = askForWhat()
        val whatToCalculate = askWhatToShow()
        val arrayDates = getDates(askDate().toInt())

        when (community) {
            1 -> {
                if (!isGraphics)
                    print(generateCountryTable(arrayDates[0], arrayDates[1], whatToCalculate))
                else {
                    val tableToPlot = generateCountryTable(arrayDates[0], arrayDates[1], whatToCalculate)!!
                    val values: MutableList<String> = ArrayList()
                    for (r: Row in tableToPlot) {
                        values.add("Nacional")
                    }
                    tableToPlot.addColumns(StringColumn.create("CCAA", values))
                    drawBarPlot(tableToPlot, whatToCalculate, askForElement())
                }
            }
            2 -> {
                val selectedCommunities = this.communities.selectCommunities()
                if (!isGraphics)
                    println(generateCommunityTable(selectedCommunities, arrayDates[0], arrayDates[1], whatToCalculate))
                else {
                    val tableToPlot = generateCommunityTable(selectedCommunities, arrayDates[0], arrayDates[1], whatToCalculate)!!
                    // Plot tableToPlot
                    drawBarPlot(tableToPlot, whatToCalculate, askForElement())
                }
                this.communities.clearLists()

            }
            3 -> {
                val selectedCommunities = this.communities.selectCommunities()
                val tempTable = generateCountryTable(arrayDates[0], arrayDates[1], whatToCalculate)
                val columna: StringColumn
                columna = if (whatToCalculate != 2) {
                    StringColumn.create("CCAA", "Nacional")
                } else {
                    val values = mutableListOf("Nacional", "Nacional", "Nacional", "Nacional", "Nacional",
                            "Nacional", "Nacional")
                    StringColumn.create("CCAA", values)
                }
                tempTable?.addColumns(columna)
                tempTable?.append(generateCommunityTable(selectedCommunities, arrayDates[0], arrayDates[1], whatToCalculate))
                if (tempTable != null) {
                    if (!isGraphics)
                        println(tempTable.printAll())
                    else {
                        drawBarPlot(tempTable, whatToCalculate, askForElement())
                    }
                }
                this.communities.clearLists()
            }
        }
    }

    private fun getDates(whatType: Int): Array<LocalDate> {
        val initialDate: LocalDate
        val finalDate: LocalDate
        when (whatType) {
            1 -> {
                initialDate = selectDate(needsTwoDates = true, isSecondDate = false, firstDate = null)
                finalDate = selectDate(needsTwoDates = true, isSecondDate = true, firstDate = initialDate)
            }
            2 -> {
                initialDate = selectDate(needsTwoDates = false, isSecondDate = false, firstDate = null)
                finalDate = initialDate
            }
            else -> {
                initialDate = this.table.row(0).getDate("FECHA")
                finalDate = this.table.row(this.table.rowCount() - 1).getDate("FECHA")
            }
        }
        return arrayOf(initialDate, finalDate)
    }

    fun getWhereToShowGraphics() {
        val dataToShow = askForWhat()
        val arrayDates = getDates(askDate().toInt())
        val field = askForElement()
        timeSerieGraphic(getTableForGraphic(dataToShow, arrayDates[0], arrayDates[1]), field)
    }

    private fun askForWhat(): Int {
        println("¿Qué datos te gustaría ver?\n")
        println("1) Nacionales")
        println("2) Comunidades")
        println("3) Nacionales vs Comunidades")
        var answer = ""
        var control = "C"
        while (control.toUpperCase() != "S") {
            answer = readLine()!!
            when (answer) {
                "1", "2", "3" -> control = "S"
                else -> println("El valor no se reconoce como válido")
            }
        }
        return answer.toInt()
    }


    private fun drawBarPlot(t: Table, whatToShow: Int, field: String) {
        val values = arrayOf("Casos", "Altas", "PCR+", "TestAc+", "Hospitalizados", "UCI", "Fallecidos")
        val textTitle: String
        val column: String
        when (whatToShow) {
            1 -> {
                textTitle = "Media de ${values[field.toInt() - 1]}"
                column = "mean [${values[field.toInt() - 1]}]"
            }
            2 -> {
                textTitle = "Máximo de ${values[field.toInt() - 1]}"
                column = "Máximo"
            }
            else -> {
                textTitle = "Total de ${values[field.toInt() - 1]}"
                column = "Sum [${values[field.toInt() - 1]}]"
            }
        }
        Plot.show(HorizontalBarPlot.create(textTitle, t, "CCAA", column))
    }

    private fun askForElement(): String {
        val values = arrayOf("Casos", "Altas", "PCR+", "TestAc+", "Hospitalizados", "UCI", "Fallecidos")

        var answer = "C"
        println("\n\n¿Que elemento quieres dibujar\n")
        while (answer != "S") {
            values.forEachIndexed { index, value -> print("${index + 1}) $value \t") }
            answer = readLine()!!
            when (answer) {
                "1", "2", "3", "4", "5", "6", "7" -> return answer
                else -> println("El valor introducido no es correcto\n")
            }
        }
        return answer
    }

    private fun getTableForGraphic(typeData: Int, initialDate: LocalDate, finalDate: LocalDate): Table? {
        return when (typeData) {
            1 -> generateCountryTimeSerieGraphic(initialDate, finalDate)
            2 -> {
                val list = this.communities.selectCommunities()
                generateCommunityTimeSerieGraphic(list, initialDate, finalDate)
            }
            else -> {
                val list = this.communities.selectCommunities()
                generateCommunityTimeSerieGraphic(list, initialDate, finalDate)?.append(generateCountryTimeSerieGraphic(initialDate, finalDate))
            }
        }
    }

    private fun timeSerieGraphic(t: Table?, field: String) {
        val columnValues = arrayOf("Casos", "Altas", "PCR+", "TestAc+", "Hospitalizados", "UCI", "Fallecidos")
        Plot.show(
                TimeSeriesPlot.create(columnValues[field.toInt() - 1] + " in a time", t,
                        "FECHA", columnValues[field.toInt() - 1], "CCAA"))
    }

    private fun generateCommunityTimeSerieGraphic(communityList: List<String>, initialDate: LocalDate, finalDate: LocalDate): Table? {
        var tableForGraphics = initialTable.emptyCopy()
        for (c in communityList) {
            var auxTable = this.initialTable.where(this.initialTable.stringColumn("CCAA").isEqualTo(c))
            auxTable = auxTable.where(auxTable.dateColumn("FECHA").isBetweenIncluding(initialDate, finalDate))
            tableForGraphics = tableForGraphics.append(auxTable)
        }
        return tableForGraphics
    }

    private fun generateCountryTimeSerieGraphic(initialDate: LocalDate, finalDate: LocalDate): Table? {

        val columnValues = arrayOf("Casos", "PCR+", "TestAc+", "Altas", "Hospitalizados", "Fallecidos", "UCI")

        var auxTable = this.initialTable.where(this.initialTable.dateColumn("FECHA").isBetweenIncluding(initialDate, finalDate))
        var tableForGraphics = auxTable.summarize("Casos", "Altas", "PCR+",
                "TestAc+", AggregateFunctions.sum).by("FECHA")
        auxTable = auxTable.summarize("Hospitalizados",
                "UCI", "Fallecidos", AggregateFunctions.sum).by("FECHA")
        tableForGraphics.sortOn("FECHA")
        auxTable.sortOn("FECHA")
        auxTable.removeColumns("FECHA")
        tableForGraphics = tableForGraphics.concat(auxTable)
        val values: MutableList<String> = ArrayList()
        for (r: Row in tableForGraphics) {
            values.add("Nacional")
        }
        tableForGraphics.addColumns(StringColumn.create("CCAA", values))

        for (c in 1 until 8) {
            tableForGraphics.column(c).setName(columnValues[c - 1])
        }

        return tableForGraphics
    }
}
