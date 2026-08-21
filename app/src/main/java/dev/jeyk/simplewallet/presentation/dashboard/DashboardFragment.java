package dev.jeyk.simplewallet.presentation.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import dev.jeyk.simplewallet.R;
import dev.jeyk.simplewallet.databinding.FragmentDashboardBinding;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.UiState;
import dev.jeyk.simplewallet.presentation.navigation.WalletNavigator;

@AndroidEntryPoint
public final class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private AccountAdapter accountAdapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        accountAdapter = new AccountAdapter(account ->
                ((WalletNavigator) requireActivity()).showStatement(account.getId())
        );
        binding.dashboardAccountsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.dashboardAccountsList.setAdapter(accountAdapter);
        binding.dashboardAccountsList.setHasFixedSize(true);
        binding.dashboardSwipeRefresh.setColorSchemeResources(R.color.wallet_primary);
        binding.dashboardSwipeRefresh.setProgressBackgroundColorSchemeResource(
                R.color.wallet_surface
        );
        binding.dashboardSwipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                binding.dashboardAccountsList.canScrollVertically(-1)
        );
        binding.dashboardSwipeRefresh.setOnRefreshListener(viewModel::refreshAccounts);
        binding.dashboardRetryButton.setOnClickListener(ignored -> viewModel.loadAccounts());
        binding.dashboardLogoutButton.setOnClickListener(ignored -> viewModel.logout());

        viewModel.getAccountsState().observe(getViewLifecycleOwner(), this::renderAccounts);
        viewModel.getAccountsRefreshing().observe(
                getViewLifecycleOwner(),
                refreshing -> binding.dashboardSwipeRefresh.setRefreshing(
                        Boolean.TRUE.equals(refreshing)
                )
        );
        viewModel.getAccountsRefreshFailureEvents().observe(
                getViewLifecycleOwner(),
                this::handleAccountsRefreshFailure
        );
        viewModel.getLogoutLoading().observe(getViewLifecycleOwner(), this::renderLogoutLoading);
        viewModel.getLogoutEvents().observe(getViewLifecycleOwner(), this::handleLogoutEvent);

        UiState<List<WalletAccount>> currentState = viewModel.getAccountsState().getValue();
        if (currentState == null || currentState.getStatus() == UiState.Status.IDLE) {
            viewModel.loadAccounts();
        }
    }

    private void renderAccounts(UiState<List<WalletAccount>> state) {
        binding.dashboardAccountsList.setVisibility(View.GONE);
        if (!Boolean.TRUE.equals(viewModel.getLogoutLoading().getValue())) {
            binding.dashboardProgressPanel.setVisibility(View.GONE);
        }
        binding.dashboardStatePanel.setVisibility(View.GONE);
        binding.dashboardRetryButton.setVisibility(View.GONE);

        switch (state.getStatus()) {
            case IDLE:
                break;
            case LOADING:
                showFullScreenLoading(R.string.accounts_loading);
                break;
            case SUCCESS:
                accountAdapter.submitList(state.getData());
                binding.dashboardAccountsList.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                accountAdapter.submitList(Collections.emptyList());
                showState(
                        R.string.accounts_empty_title,
                        R.string.accounts_empty_message,
                        false
                );
                break;
            case ERROR:
                accountAdapter.submitList(Collections.emptyList());
                showState(
                        R.string.accounts_error_title,
                        R.string.accounts_error_message,
                        true
                );
                break;
        }
    }

    private void showState(int titleResource, int messageResource, boolean showRetry) {
        binding.dashboardStateTitle.setText(titleResource);
        binding.dashboardStateMessage.setText(messageResource);
        binding.dashboardRetryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        binding.dashboardStatePanel.setVisibility(View.VISIBLE);
    }

    private void renderLogoutLoading(Boolean loading) {
        boolean isLoading = Boolean.TRUE.equals(loading);
        binding.dashboardLogoutButton.setEnabled(!isLoading);
        binding.dashboardLogoutButton.setText(
                isLoading ? R.string.logout_loading : R.string.logout_action
        );
        if (isLoading) {
            showFullScreenLoading(R.string.logout_loading);
            return;
        }

        UiState<List<WalletAccount>> accountsState = viewModel.getAccountsState().getValue();
        if (accountsState != null && accountsState.getStatus() == UiState.Status.LOADING) {
            showFullScreenLoading(R.string.accounts_loading);
        } else {
            binding.dashboardProgressPanel.setVisibility(View.GONE);
        }
    }

    private void showFullScreenLoading(int messageResource) {
        String message = getString(messageResource);
        binding.dashboardLoadingText.setText(message);
        binding.dashboardProgressPanel.setContentDescription(message);
        binding.dashboardProgressPanel.setVisibility(View.VISIBLE);
    }

    private void handleLogoutEvent(SingleEvent<Boolean> event) {
        Boolean success = event.consume();
        if (success == null) {
            return;
        }
        if (success) {
            ((WalletNavigator) requireActivity()).showLoginAfterLogout();
        } else {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.generic_error_message,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void handleAccountsRefreshFailure(SingleEvent<Boolean> event) {
        if (Boolean.TRUE.equals(event.consume())) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.generic_error_message,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onDestroyView() {
        binding.dashboardSwipeRefresh.setOnRefreshListener(null);
        binding.dashboardSwipeRefresh.setOnChildScrollUpCallback(null);
        binding.dashboardAccountsList.setAdapter(null);
        accountAdapter = null;
        binding = null;
        super.onDestroyView();
    }
}
