package com.example.yolo_homes.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yolo_homes.data.SessionManager
import com.example.yolo_homes.data.model.Post
import com.example.yolo_homes.data.repository.AuthRepository
import com.example.yolo_homes.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostsUiState(
    val loading: Boolean = true,
    val posts: List<Post> = emptyList(),
    val isAdmin: Boolean = false,
    val myName: String = ""
)

sealed interface PostSaveState {
    data object Idle : PostSaveState
    data object Saving : PostSaveState
    data object Saved : PostSaveState
    data class Error(val message: String) : PostSaveState
}

/** The Home screen's "Community Board" — news/events/notices, independent of Maintenance/Water. */
@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<PostsUiState> = combine(
        postRepository.observePosts(),
        sessionManager.session
    ) { posts, session ->
        PostsUiState(
            loading = false,
            posts = posts,
            isAdmin = session?.isAdmin == true,
            myName = session?.user?.displayName.orEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PostsUiState())

    private val _saveState = MutableStateFlow<PostSaveState>(PostSaveState.Idle)
    val saveState: StateFlow<PostSaveState> = _saveState.asStateFlow()

    fun createPost(title: String, body: String, type: String, eventDateMillis: Long?, pinned: Boolean) {
        viewModelScope.launch {
            _saveState.value = PostSaveState.Saving
            runCatching {
                postRepository.createPost(
                    title = title,
                    body = body,
                    type = type,
                    eventDateMillis = eventDateMillis,
                    pinned = pinned,
                    createdByUid = authRepository.currentUid ?: "",
                    createdByName = uiState.value.myName.ifBlank { "Admin" }
                )
            }.onSuccess { _saveState.value = PostSaveState.Saved }
                .onFailure { _saveState.value = PostSaveState.Error(it.message ?: "Post failed") }
        }
    }

    fun deletePost(id: String) {
        viewModelScope.launch { runCatching { postRepository.deletePost(id) } }
    }

    fun clearSaveState() { _saveState.value = PostSaveState.Idle }
}
