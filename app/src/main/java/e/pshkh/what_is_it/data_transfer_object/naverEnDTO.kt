package e.pshkh.what_is_it.data_transfer_object

data class naverEnDTO(
    var description: String? = "",
    var total: Int? = 0,
    var items: MutableList<item>? = null
){
    data class item(
        var title: String? = "",
        var link: String? = "",
        var description: String? = ""
    )
}