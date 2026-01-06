package elfak.mosis.myplaces.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import elfak.mosis.myplaces.MainActivity
import elfak.mosis.myplaces.R
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.model.UserViewModel


class LoginFragment : Fragment(R.layout.fragment_login) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val authRepo = AuthRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.emailInput)
        val pass = view.findViewById<EditText>(R.id.passwordInput)

        // Call auth Repo login with fragment data
        view.findViewById<Button>(R.id.loginBtn).setOnClickListener {
            // On success update the view model and navigate to home , onErr display error msg
            authRepo.login(
                email.text.toString(),
                pass.text.toString(),
                onSuccess = { appUser ->
                    findNavController().navigate(R.id.action_login_to_home)
                    userViewModel.currentUser.value = appUser
                },
                onError = { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            )
        }

        // Navigate back to Register screen
        view.findViewById<TextView>(R.id.goRegister).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }
}
