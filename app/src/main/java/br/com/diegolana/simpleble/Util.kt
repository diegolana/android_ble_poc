package br.com.diegolana.simpleble

class Util {

    companion object {
        fun byteToInt(bytes: ByteArray?): Int {
            var result = 0
            bytes?.let {
                for (i in bytes.indices) {
                    result = result or (bytes[i].toInt() shl 8 * i)
                }
            }
            return result
        }
    }
}