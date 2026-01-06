package elfak.mosis.myplaces

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.auth.AuthRepository
import elfak.mosis.myplaces.model.UserViewModel


// Primary activity that starts on app startup checks if an user is already logged in
class AuthActivity : AppCompatActivity() {

    private val authRepo = AuthRepository()
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.auth_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            authRepo.fetchAppUser(currentUser.uid,
                onSuccess = { appUser ->
                    userViewModel.currentUser.value = appUser
                    navController.navigate(R.id.HomeFragment)
                },
                onError = { msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    navController.navigate(R.id.LoginFragment)
                }
            )
        }
    }
}

