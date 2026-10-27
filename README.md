# J.A.R.V.I.S. Android — projeto atualizado

Projeto Android com interface HUD futurista em HTML, conexão com o Worker Cloudflare configurado, reconhecimento de voz nativo e fala por TextToSpeech do Android. A fala usa o mecanismo TTS padrão definido nas configurações do celular (por exemplo, HayaiTTS, se estiver selecionado e instalado).

## Compilar pelo GitHub no celular
1. Extraia este ZIP.
2. Envie o conteúdo da pasta `JARVIS-Android-Projeto` para a raiz do repositório GitHub `codeghost444/JARVIS-Android`, substituindo os arquivos existentes. A pasta `.github` deve ir junto.
3. Abra **Actions** no GitHub e execute **Build JARVIS APK** (ou aguarde o build após o push).
4. Abra a execução concluída e baixe o artefato **JARVIS-APK**. Extraia o ZIP do artefato e instale `app-debug.apk`.

## Configuração incluída
- Worker: `https://still-grass-fbf7.bartmeuadriano.workers.dev/`
- Modelo configurado no HTML: `nvidia/nemotron-3.5-super-vl-120b-a12b`
- Java 17, Android Gradle Plugin 8.7.3 e Gradle 8.9.
- Permissões de internet e microfone.
- Ponte Android para voz nativa e mensagens de diagnóstico do WebView.

## Importante
A chave da IA deve ficar como variável secreta no Worker Cloudflare; não coloque chave privada no HTML nem no GitHub. A fala depende do mecanismo TTS instalado e configurado no Android. O projeto não implementa escuta contínua com tela apagada nem serviço de voz em segundo plano. Este ZIP foi preparado e verificado estruturalmente, mas a compilação final deve ser confirmada pelo GitHub Actions e pelo teste no aparelho.
