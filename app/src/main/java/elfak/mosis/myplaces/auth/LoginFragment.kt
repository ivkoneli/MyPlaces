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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import elfak.mosis.myplaces.MainActivity
import elfak.mosis.myplaces.R
import com.google.firebase.auth.FirebaseAuth


class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.emailInput)
        val pass = view.findViewById<EditText>(R.id.passwordInput)

        view.findViewById<Button>(R.id.loginBtn).setOnClickListener {
            val e = email.text.toString()
            val p = pass.text.toString()

            if (e.isBlank() || p.isBlank()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(
                email.text.toString(),
                pass.text.toString()
            ).addOnSuccessListener {
                findNavController().navigate(R.id.action_login_to_home)
            }.addOnFailureListener { e ->
                Log.e("AUTH", "Login failed", e)
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }

        view.findViewById<TextView>(R.id.goRegister).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }
}
