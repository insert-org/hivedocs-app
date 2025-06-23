package com.insert.hivedocs.data

object KnowledgeBase {
    val qna: Map<List<String>, String> = mapOf(
        listOf("o que é", "hivedocs", "sobre", "finalidade") to "O HiveDocs é uma plataforma colaborativa para submissão e avaliação de artigos acadêmicos. Usuários podem ler, enviar, avaliar e discutir o conteúdo.",
        listOf("quem pode usar", "tipos de usuário", "perfis") to "O aplicativo possui dois tipos de usuários: Usuários Comuns (leitores/autores) e Administradores (moderadores).",
        listOf("criar conta", "registrar", "cadastro") to "Na tela de login, você pode se registrar usando um e-mail e senha ou fazer login diretamente com sua conta Google.",

        listOf("ler artigos", "ver artigos", "lista") to "Na aba 'Artigos', você encontrará a lista de todos os trabalhos que já foram aprovados pela moderação. Toque em um para ver seus detalhes.",
        listOf("enviar artigo", "submeter", "novo artigo") to "Na tela principal 'Artigos', toque no botão de '+' no canto superior direito. Isso abrirá o formulário para você preencher os dados e submeter seu novo artigo para aprovação.",
        listOf("artigo não apareceu", "meu artigo sumiu", "status") to "Todo artigo enviado precisa ser aprovado por um administrador para aparecer na lista principal. Por favor, aguarde o processo de revisão.",
        listOf("link do artigo", "url", "fonte") to "Se o autor do artigo incluiu um link durante a submissão, ele estará disponível na página de detalhes do artigo como um link clicável 'Acessar artigo original'.",

        listOf("avaliar", "dar nota", "estrelas", "rating") to "Na página de detalhes de um artigo, na seção 'Deixe sua avaliação', você pode clicar nas estrelas para dar uma nota, escrever um comentário e clicar em 'Enviar Avaliação'.",
        listOf("editar avaliação", "excluir avaliação", "apagar comentário") to "Sim. Na sua própria avaliação, clique no menu de opções (três pontinhos) para encontrar as opções 'Editar' e 'Excluir'.",
        listOf("responder", "resposta", "editar resposta", "excluir resposta") to "Sim, abaixo de cada avaliação há um botão 'Responder'. Após postar uma resposta, você pode editá-la ou excluí-la usando o menu de opções ao lado dela. Você também pode denunciar respostas de outros usuários.",
        listOf("denunciar", "reportar", "conteúdo impróprio") to "Se você encontrar uma avaliação ou resposta inadequada, clique no menu de opções (três pontinhos) ao lado dela e selecione 'Denunciar'. Um diálogo aparecerá para você descrever o motivo, que será enviado para a moderação.",

        listOf("administrador", "admin", "o que faz", "moderação") to "Administradores têm acesso à aba 'Moderação', que é um painel central. De lá, podem acessar a área de 'Aprovação de Artigos' e a de 'Conteúdo Denunciado' para gerenciar denúncias, excluir conteúdo e banir usuários.",
        listOf("aprovar artigo", "rejeitar") to "Administradores devem ir à aba 'Moderação' e selecionar a opção 'Aprovação de Artigos'. Lá encontrarão a lista de artigos pendentes com os botões para Aprovar ou Rejeitar.",
        listOf("banir", "banimento") to "Na tela de 'Conteúdo Denunciado', o administrador tem a opção de banir o autor do conteúdo inadequado. Ao banir, o usuário perde o acesso ao aplicativo e todas as suas avaliações e respostas são removidas.",

        listOf("sair", "logout", "deslogar") to "Para sair da sua conta, vá para a aba 'Perfil' e clique no botão vermelho 'Sair da Conta'.",
        listOf("alterar senha", "mudar senha", "esqueci a senha") to "Se você fez login com e-mail e senha, pode alterar sua senha na aba 'Perfil'. Para contas Google, a recuperação é gerenciada pelo próprio Google. A função de 'Esqueci minha senha' na tela de login ainda não foi implementada.",
        listOf("como ser admin", "virar administrador") to "O status de administrador é concedido manualmente pela equipe de gerenciamento do HiveDocs.",
        listOf("fui banido", "conta banida") to "Se sua conta foi banida por um administrador, o acesso ao aplicativo é bloqueado. O banimento ocorre por violação das regras da comunidade e resulta na remoção de todo o seu conteúdo.",

        listOf("quem é você", "chatbot", "bee", "assistente") to "Eu sou o Bee 🐝, o assistente virtual do HiveDocs! Minha inteligência é fornecida pela API Gemini do Google e fui treinado para responder perguntas sobre como usar todas as funcionalidades deste aplicativo."
    )
}