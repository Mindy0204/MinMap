package com.mindyhsu.minmap.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.mindyhsu.minmap.databinding.FragmentLoginBinding
import com.mindyhsu.minmap.ext.getVmFactory
import com.mindyhsu.minmap.map.MapFragmentDirections

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val viewModel by viewModels<LoginViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        viewModel.isSignIn.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigate(MapFragmentDirections.navigateToMapFragment())
            }
        }

        binding.loginButton.setOnClickListener {
            startActivityForResult(
                viewModel.googleSignInClient.signInIntent,
                GOOGLE_SING_IN_RESULT
            )
        }

        viewModel.error.observe(viewLifecycleOwner) {
            if (it != null) {
                Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent
        if (requestCode === GOOGLE_SING_IN_RESULT) {
            // The Task returned from this call is always completed, no need to attach a listener
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            viewModel.handleSignInResult(task)
        }
    }

    companion object {
        const val GOOGLE_SING_IN_RESULT = 1
    }
}
