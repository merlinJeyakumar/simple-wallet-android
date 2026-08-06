package dev.jeyk.simplewallet.presentation.statement;

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

import dev.jeyk.simplewallet.AppContainer;
import dev.jeyk.simplewallet.R;
import dev.jeyk.simplewallet.SimpleWalletApplication;
import dev.jeyk.simplewallet.databinding.FragmentStatementBinding;
import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.presentation.common.UiState;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.WalletFormatters;
import dev.jeyk.simplewallet.presentation.common.WalletViewModelFactory;
import dev.jeyk.simplewallet.presentation.navigation.WalletNavigator;

public final class StatementFragment extends Fragment {
    private static final String ARG_ACCOUNT_ID = "account_id";

    private FragmentStatementBinding binding;
    private StatementViewModel viewModel;
    private TransactionAdapter transactionAdapter;

    public static StatementFragment newInstance(String accountId) {
        StatementFragment fragment = new StatementFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_ACCOUNT_ID, accountId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentStatementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String accountId = requireArguments().getString(ARG_ACCOUNT_ID);
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalStateException("Statement requires an account id");
        }

        AppContainer container = ((SimpleWalletApplication) requireActivity().getApplication())
                .getContainer();
        WalletViewModelFactory factory = new WalletViewModelFactory(() -> new StatementViewModel(
                accountId,
                container.getGetAccountStatementUseCase(),
                container.getRequestDelay(),
                container.getExecutorService()
        ));
        viewModel = new ViewModelProvider(this, factory).get(StatementViewModel.class);

        transactionAdapter = new TransactionAdapter();
        binding.statementTransactionsList.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.statementTransactionsList.setAdapter(transactionAdapter);
        binding.statementTransactionsList.setHasFixedSize(true);
        binding.statementSwipeRefresh.setColorSchemeResources(R.color.wallet_primary);
        binding.statementSwipeRefresh.setProgressBackgroundColorSchemeResource(
                R.color.wallet_surface
        );
        binding.statementSwipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                binding.statementTransactionsList.canScrollVertically(-1)
        );
        binding.statementSwipeRefresh.setOnRefreshListener(viewModel::refreshStatement);
        binding.statementToolbar.setNavigationOnClickListener(ignored ->
                ((WalletNavigator) requireActivity()).goBack()
        );
        binding.statementRetryButton.setOnClickListener(ignored -> viewModel.loadStatement());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getRefreshing().observe(
                getViewLifecycleOwner(),
                refreshing -> binding.statementSwipeRefresh.setRefreshing(
                        Boolean.TRUE.equals(refreshing)
                )
        );
        viewModel.getRefreshFailureEvents().observe(
                getViewLifecycleOwner(),
                this::handleRefreshFailure
        );
        UiState<AccountStatement> currentState = viewModel.getState().getValue();
        if (currentState == null || currentState.getStatus() == UiState.Status.IDLE) {
            viewModel.loadStatement();
        }
    }

    private void render(UiState<AccountStatement> state) {
        binding.statementTransactionsList.setVisibility(View.GONE);
        binding.statementProgressPanel.setVisibility(View.GONE);
        binding.statementStatePanel.setVisibility(View.GONE);
        binding.statementRetryButton.setVisibility(View.GONE);
        binding.statementSummaryCard.setVisibility(View.INVISIBLE);
        binding.statementSectionTitle.setVisibility(View.INVISIBLE);

        switch (state.getStatus()) {
            case IDLE:
                break;
            case LOADING:
                transactionAdapter.submitList(Collections.emptyList());
                binding.statementProgressPanel.setVisibility(View.VISIBLE);
                break;
            case SUCCESS:
                showStatement(state.getData());
                binding.statementTransactionsList.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                showStatement(state.getData());
                showState(
                        R.string.statement_empty_title,
                        R.string.statement_empty_message,
                        false
                );
                break;
            case ERROR:
                transactionAdapter.submitList(Collections.emptyList());
                showState(
                        R.string.statement_error_title,
                        R.string.statement_error_message,
                        true
                );
                break;
        }
    }

    private void showStatement(AccountStatement statement) {
        WalletAccount account = statement.getAccount();
        String formattedBalance = WalletFormatters.money(
                account.getBalance(),
                account.getCurrency()
        );
        binding.statementAccountName.setText(account.getName());
        binding.statementCurrency.setText(account.getCurrency().getCurrencyCode());
        binding.statementMaskedNumber.setText(account.getMaskedNumber());
        binding.statementBalance.setText(formattedBalance);
        binding.statementSummaryCard.setContentDescription(getString(
                R.string.account_summary_content_description,
                account.getName(),
                account.getMaskedNumber(),
                formattedBalance
        ));
        binding.statementSummaryCard.setVisibility(View.VISIBLE);
        binding.statementSectionTitle.setVisibility(View.VISIBLE);
        transactionAdapter.submitStatement(statement);
    }

    private void showState(int titleResource, int messageResource, boolean showRetry) {
        binding.statementStateTitle.setText(titleResource);
        binding.statementStateMessage.setText(messageResource);
        binding.statementRetryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        binding.statementStatePanel.setVisibility(View.VISIBLE);
    }

    private void handleRefreshFailure(SingleEvent<Boolean> event) {
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
        binding.statementSwipeRefresh.setOnRefreshListener(null);
        binding.statementSwipeRefresh.setOnChildScrollUpCallback(null);
        binding.statementTransactionsList.setAdapter(null);
        transactionAdapter = null;
        binding = null;
        super.onDestroyView();
    }
}
