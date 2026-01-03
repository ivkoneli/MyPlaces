package elfak.mosis.myplaces.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import elfak.mosis.myplaces.MainActivity
import elfak.mosis.myplaces.R

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.emailInput)
        val pass = view.findViewById<EditText>(R.id.passwordInput)
        val registerBtn = view.findViewById<Button>(R.id.registerBtn)

        registerBtn.setOnClickListener {
            val e = email.text.toString()
            val p = pass.text.toString()

            if (e.isBlank() || p.isBlank()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email.text.toString(), pass.text.toString())
                .addOnSuccessListener {
                    findNavController().navigate(R.id.action_register_to_home)
                }
        }

        view.findViewById<TextView>(R.id.goLogin).setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}
