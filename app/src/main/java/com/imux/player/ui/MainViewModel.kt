package com.imux.player.ui
import androidx.lifecycle.*
import com.imux.player.ImuxApplication
import com.imux.player.data.Track
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class MainViewModel(private val a:ImuxApplication):ViewModel(){val tracks=a.library.tracks.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList());val folders=a.library.folders.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList());val onboarding=a.settings.onboarding.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),false);fun addFolder(u:String,n:String)=viewModelScope.launch{a.library.addFolder(u,n);a.settings.done()};fun scan()=viewModelScope.launch{a.library.scanAll()};fun favorite(t:Track)=viewModelScope.launch{a.library.favorite(t.uri,!t.favorite)};companion object{fun factory(a:ImuxApplication)=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:ViewModel>create(c:Class<T>)=MainViewModel(a) as T}}}