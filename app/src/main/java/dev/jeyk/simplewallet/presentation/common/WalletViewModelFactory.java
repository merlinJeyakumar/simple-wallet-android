package dev.jeyk.simplewallet.presentation.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;
import java.util.function.Supplier;

public final class WalletViewModelFactory implements ViewModelProvider.Factory {
    private final Supplier<? extends ViewModel> supplier;

    public WalletViewModelFactory(Supplier<? extends ViewModel> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ViewModel viewModel = supplier.get();
        if (!modelClass.isInstance(viewModel)) {
            throw new IllegalArgumentException("Unsupported ViewModel: " + modelClass.getName());
        }
        return (T) viewModel;
    }
}
