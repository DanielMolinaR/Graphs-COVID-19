class Communities {
    private val communities: List<String> = listOf("Andalucía", "Aragón", "Asturias", "Baleares", "Canarias",
            "Cantabria", "Castilla La Mancha", "Castilla y León", "Cataluña", "Ceuta", "C. Valenciana", "Extremadura",
            "Galicia", "Madrid", "Melilla", "Murcia", "Navarra", "País Vasco", "La Rioja")
    private var selectedCommunities: MutableList<String> = ArrayList()
    private var tempCommunities = this.communities.toMutableList()

    fun selectCommunities(): MutableList<String> {
        var selection = "X"
        println("Selecciona al menos una comunidad autónoma")

        while ((selection != "C" && selection != "c") || selectedCommunities.isEmpty()) {
            println("\n\nSelecciona la comunidad que quieras comprobar")
            print(this.generatePrint())
            selection = readLine()!!
            if (isNumeric(selection))
                this.addToSelectedList(selection.toInt())
            else
                if(selection.toUpperCase() == "T") {
                    this.selectedCommunities.addAll(this.tempCommunities)
                    this.tempCommunities.clear()
                }
                else if (selection != "C" && selection != "c") {
                    print("El elemento escogido es incorrecto. Por favor selecciona un elemento de la lista\n\n\n")
                }
            if (this.tempCommunities.isEmpty())
                break
        }
        return this.selectedCommunities
    }

    private fun isNumeric(input: String): Boolean =
            try {
                input.toDouble()
                true
            } catch (e: NumberFormatException) {
                false
            }

    private fun addToSelectedList(selection: Int) {
        if (this.tempCommunities.size >= selection)
            this.selectedCommunities.add(this.tempCommunities.removeAt(selection - 1))
        else
            print("El elemento escogido es incorrecto. Por favor selecciona un elemento de la lista\n\n\n")
    }

    private fun generatePrint(): String {
        var communityIndex = 1
        var stringCommunities = ""

        for (community in this.tempCommunities) {
            stringCommunities += "$communityIndex) $community  "
            communityIndex++


            if (communityIndex % 5 == 0)
                stringCommunities += "\n"
        }
        stringCommunities += "Selecciona T para añadir todas las comunidades autónomas"

        return "\n$stringCommunities\nCuando hayas seleccionado todas las comunidades que quieras, selecciona C para " +
                "continuar la ejecución\n"
    }

    fun clearLists() {
        this.selectedCommunities = ArrayList()
        this.tempCommunities = this.communities.toMutableList()
    }

}