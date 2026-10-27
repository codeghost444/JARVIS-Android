# J.A.R.V.I.S. Android — projeto para compilar APK

Este projeto inclui a interface HTML do J.A.R.V.I.S. em `app/src/main/assets/index.html`, uma Activity Android baseada em WebView e uma ação do GitHub Actions para compilar o APK de depuração.

## Gerar o APK pelo celular

1. Entre em https://github.com e crie um repositório **Private** chamado `JARVIS-Android`.
2. Envie todos os arquivos e pastas deste projeto para a raiz do repositório. A pasta `.github/workflows` precisa ser enviada também. No celular, pode ser mais simples enviar o ZIP pelo site do GitHub se a interface oferecer a opção de upload; se enviar o ZIP como um único arquivo, o workflow não encontrará o projeto — os arquivos precisam estar extraídos na raiz do repositório.
3. Abra a aba **Actions** no repositório. Se necessário, confirme que deseja habilitar workflows.
4. Selecione **Build JARVIS APK** e toque em **Run workflow**, ou faça um commit/push na branch `main`.
5. Quando a execução terminar com sucesso, abra a execução e baixe o artefato **JARVIS-APK**. Extraia o ZIP do artefato e instale `app-debug.apk` no Android.

## Notas importantes

- O projeto usa Java 17, Gradle 8.9 e Android Gradle Plugin 8.7.3.
- A URL do Cloudflare Worker e o modelo estão definidos no HTML. A chave da IA deve permanecer configurada no Worker; não coloque a chave secreta no HTML nem no repositório.
- O reconhecimento de voz usa a API de voz do navegador quando disponível e tem um fallback para o reconhecimento nativo do Android. O resultado depende dos serviços de voz instalados no telefone e da permissão do microfone.
- A fala de resposta usa `speechSynthesis` do WebView. A disponibilidade/voz em português depende do mecanismo de síntese de fala instalado no Android.
- Esta versão **não mantém escuta contínua com a tela apagada nem possui serviço em segundo plano**. A conversa contínua na interface funciona enquanto o aplicativo estiver aberto e o Android permitir o microfone.
- O projeto foi preparado para a compilação em nuvem, mas não foi compilado/testado num dispositivo neste ambiente. Se o workflow apresentar erro, envie o log da execução para corrigirmos.
