package com.wl.turbidimetric.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wl.turbidimetric.app.App
import com.wl.turbidimetric.app.AppViewModel
import com.wl.turbidimetric.base.BaseViewModel
import com.wl.turbidimetric.db.ServiceLocator
import com.wl.turbidimetric.ex.getAppViewModel
import com.wl.turbidimetric.model.UserModel
import com.wl.turbidimetric.repository.if2.LocalDataSource
import com.wl.turbidimetric.repository.if2.LogListDataSource
import com.wl.turbidimetric.repository.if2.UserSource
import com.wl.wllib.LogToFile.i
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val appViewModel: AppViewModel,
    private val localDataRepository: LocalDataSource,
    private val userSource: UserSource,
    private val logListDataSource: LogListDataSource
) : BaseViewModel() {

    private val _uiState = MutableSharedFlow<LoginUiState>()
    val uiState: SharedFlow<LoginUiState> = _uiState.asSharedFlow()

    fun getPrevUserName(): String {
        return localDataRepository.getPrevUserName()
    }
    fun getPrevUserPsw(): String {
        return localDataRepository.getPrevUserPsw()
    }

    fun setPrevPsw(psw: String) {
        return localDataRepository.setPrevUserPsw(psw)
    }
    fun setPrevUserName(userName: String) {
        return localDataRepository.setPrevUserName(userName)
    }

    fun login(userName: String, password: String) {
        if (!verifyParams(userName, password)) {
            return;
        }
        viewModelScope.launch {
            val um = userSource.login(userName, password)
            if (um == null) {
                loginFailed("登录失败，请检测账号密码")
            } else {
                loginSuccess(um)
            }
        }
    }

    private fun loginFailed(msg: String) {
        viewModelScope.launch {
            _uiState.emit(LoginUiState.Failed(msg))
            i(msg)
        }
    }

    private fun loginSuccess(um: UserModel) {
        appViewModel.userModel = um
        setPrevUserName(um.userName)
        setPrevPsw(um.password)
        viewModelScope.launch {
            _uiState.emit(LoginUiState.Success("登录成功"))
            i("登录成功")
        }
    }

    private fun verifyParams(userName: String, password: String): Boolean {
        if (userName.isNullOrEmpty() || password.isNullOrEmpty()) {
            loginFailed("用户名或密码不能为空")
            return false
        }
        return true
    }

    fun processIntent(intent: LoginIntent) {
        viewModelScope.launch {
            when (intent) {
                is LoginIntent.Login -> {
                    login(intent.userName, intent.password)
                }

                else -> {

                }
            }
        }
    }

    sealed class LoginIntent {
        data class Login(val userName: String, val password: String) : LoginIntent()
    }

    sealed class LoginUiState {
        data class Success(val msg: String = "") : LoginUiState()
        data class Failed(val msg: String = "") : LoginUiState()
    }


    class LoginViewModelFactory(
        private val appViewModel: AppViewModel = getAppViewModel(AppViewModel::class.java),
        private val localDataRepository: LocalDataSource = ServiceLocator.provideLocalDataSource(App.instance!!),
        private val userSource: UserSource = ServiceLocator.provideUserSource(App.instance!!),
        private val logListDataSource: LogListDataSource = ServiceLocator.providerLogListDataSource(
            App.instance!!
        )
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(
                    appViewModel,
                    localDataRepository,
                    userSource,
                    logListDataSource
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
