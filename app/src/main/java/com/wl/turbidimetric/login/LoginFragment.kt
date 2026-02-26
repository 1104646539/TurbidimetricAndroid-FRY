package com.wl.turbidimetric.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.wl.turbidimetric.R
import com.wl.turbidimetric.base.BaseFragment
import com.wl.turbidimetric.databinding.FragmentLoginBinding
import com.wl.turbidimetric.datastore.LocalDataGlobal
import com.wl.turbidimetric.ex.toast
import com.wl.turbidimetric.global.EventGlobal
import com.wl.turbidimetric.global.EventMsg
import com.wl.turbidimetric.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.greenrobot.eventbus.EventBus

class LoginFragment : BaseFragment<LoginViewModel, FragmentLoginBinding>(R.layout.fragment_login) {
    override val vm: LoginViewModel by viewModels { LoginViewModel.LoginViewModelFactory() }

    override fun initViewModel() {

    }

    override fun init(savedInstanceState: Bundle?) {
        vd.etUserName.setText(vm.getPrevUserName())
        vd.etPassword.setText(vm.getPrevUserPsw())
        vd.btnLogin.setOnClickListener {
            vm.processIntent(
                LoginViewModel.LoginIntent.Login(
                    vd.etUserName.text.toString(),
                    vd.etPassword.text.toString()
                )
            )
        }
        vd.etPassword.setOnEditorActionListener { v, actionId, e ->
            if (e.action == KeyEvent.ACTION_DOWN) {
                vd.btnLogin.requestFocus()
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(vd.etPassword.windowToken, 0)
                true
            } else {
                false
            }
        }
        lifecycleScope.launch {
            vm.uiState.collectLatest {
                when (it) {
                    is LoginViewModel.LoginUiState.Success -> {
                        startMainUI()
                    }

                    is LoginViewModel.LoginUiState.Failed -> {
                        toast(it.msg)
                    }

                    else -> {}
                }
            }
        }

    }

    private fun startMainUI() {
        EventBus.getDefault().post(EventMsg<Any>(what = EventGlobal.WHAT_LOGIN_SUCCESS))
    }
}
