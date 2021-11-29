package br.com.diegolana.simpleble

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import br.com.diegolana.simpleble.databinding.BLEDataBinding


class BLEActivity : PermissionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this).get(ViewModelBLE::class.java)
        val binding:BLEDataBinding  = DataBindingUtil.setContentView(this, R.layout.activity_ble)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.buttonScan.setOnClickListener {
            if (requestPermission()) {
                viewModel.scanLeDevice()
            }
        }
    }

}