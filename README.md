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


## Escuta em segundo plano (palavra de ativação)

A interface inclui o botão **Ativar segundo plano**. Toque nele enquanto o app está aberto e conceda acesso ao microfone. Uma notificação persistente indica que o serviço está ativo; diga **“Jarvis”** seguido do comando, por exemplo “Jarvis, que horas são?”. Toque na notificação para voltar à interface. Para desligar, volte ao app e toque em **Segundo plano: ON**.

**Limitações importantes:** esta versão usa o serviço de reconhecimento de voz disponibilizado pelo Android para procurar a palavra “Jarvis”; não é um detector acústico local dedicado. A disponibilidade, latência, internet e comportamento com a tela bloqueada dependem do serviço de reconhecimento instalado e da versão do Android. O serviço mantém o microfone ativo, pode consumir bastante bateria e mostra notificação permanente. Alguns fabricantes podem encerrar serviços; se ocorrer, permita atividade em segundo plano/uso irrestrito de bateria para o app. A ativação por palavra não é garantida em todos os aparelhos, nem substitui o hotword oficial do sistema.


## Ajustes desta versão (escuta e voz)
- Removido o botão e a lógica de conversa contínua.
- A escuta em segundo plano aguarda a palavra “Jarvis”; se o usuário disser apenas “Jarvis”, abre uma segunda etapa para capturar o comando.
- A escuta fica pausada enquanto o JARVIS processa e fala, e tenta voltar após a resposta para reduzir disputa pelo microfone.
- A síntese de voz usa explicitamente o mecanismo TTS padrão configurado no Android (por exemplo, HayaiTTS), com fallback de voz do navegador se o mecanismo nativo reportar erro.

**Limitações Android:** a detecção é baseada no serviço de reconhecimento de voz do aparelho, não em um detector local dedicado. Pode depender de internet, idioma instalado, bateria e regras do fabricante; por isso não é possível garantir reconhecimento contínuo em todo aparelho ou com a tela bloqueada.
