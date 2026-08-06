package dev.jeyk.simplewallet.presentation.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import dev.jeyk.simplewallet.AppContainer;
import dev.jeyk.simplewallet.R;
import dev.jeyk.simplewallet.SimpleWalletApplication;
import dev.jeyk.simplewallet.databinding.FragmentLoginBinding;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.WalletViewModelFactory;
import dev.jeyk.simplewallet.presentation.navigation.WalletNavigator;

public final class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppContainer container = ((SimpleWalletApplication) requireActivity().getApplication())
                .getContainer();
        WalletViewModelFactory factory = new WalletViewModelFactory(() -> new LoginViewModel(
                container.getValidateLoginUseCase(),
                container.getLoginUseCase(),
                container.getRequestDelay(),
                container.getExecutorService()
        ));
        viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        binding.loginButton.setOnClickListener(ignored -> submit());
        binding.loginPasswordInput.setOnEditorActionListener((ignored, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.loginIdentifierInput.addTextChangedListener(clearErrorsWatcher());
        binding.loginPasswordInput.addTextChangedListener(clearErrorsWatcher());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getAuthenticationEvents().observe(
                getViewLifecycleOwner(),
                this::handleAuthenticationEvent
        );
    }

    private void submit() {
        CharSequence identifier = binding.loginIdentifierInput.getText();
        CharSequence password = binding.loginPasswordInput.getText();
        viewModel.login(
                identifier == null ? "" : identifier.toString(),
                password == null ? "" : password.toString()
        );
    }

    private void render(LoginUiState state) {
        boolean loading = state.isLoading();
        binding.loginIdentifierInput.setEnabled(!loading);
        binding.loginPasswordInput.setEnabled(!loading);
        binding.loginButton.setEnabled(!loading);
        binding.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginIdentifierLayout.setError(state.getIdentifierError());
        binding.loginPasswordLayout.setError(state.getPasswordError());

        String message = state.getMessage();
        if (state.isFailure()) {
            binding.loginErrorText.setText(
                    message == null ? getString(R.string.generic_error_message) : message
            );
            binding.loginErrorText.setVisibility(View.VISIBLE);
        } else {
            binding.loginErrorText.setText(null);
            binding.loginErrorText.setVisibility(View.GONE);
        }
    }

    private void handleAuthenticationEvent(SingleEvent<Boolean> event) {
        Boolean authenticated = event.consume();
        if (Boolean.TRUE.equals(authenticated)) {
            ((WalletNavigator) requireActivity()).showDashboard();
        }
    }

    private TextWatcher clearErrorsWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (binding != null) {
                    binding.loginIdentifierLayout.setError(null);
                    binding.loginPasswordLayout.setError(null);
                    binding.loginErrorText.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
